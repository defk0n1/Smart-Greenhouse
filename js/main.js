// main.js - Main application logic for Three.js with Sketchfab-quality rendering
import * as THREE from 'three';
import { OrbitControls } from 'three/addons/controls/OrbitControls.js';
import { GLTFLoader } from 'three/addons/loaders/GLTFLoader.js';
import { EXRLoader } from 'three/addons/loaders/EXRLoader.js';
import { EffectComposer } from 'three/addons/postprocessing/EffectComposer.js';
import { RenderPass } from 'three/addons/postprocessing/RenderPass.js';
import { SSAOPass } from 'three/addons/postprocessing/SSAOPass.js';
import { UnrealBloomPass } from 'three/addons/postprocessing/UnrealBloomPass.js';
import { OutputPass } from 'three/addons/postprocessing/OutputPass.js';
import { mvp } from './mvp.js';
import { authManager } from './auth.js';

class GreenhouseApp {
    constructor() {
        this.scene = null;
        this.camera = null;
        this.renderer = null;
        this.controls = null;
        this.model = null;
        this.autoRotateEnabled = true;
        this.raycaster = null;
        this.mouse = null;
        this.sensorMeshes = {};
        this.composer = null;

        this.init();
    }

    init() {
        const container = document.getElementById('container');

        // Scene with realistic gradient sky background
        this.scene = new THREE.Scene();

        // Create realistic gradient skybox (like Sketchfab)
        // this.createSkybox(); // Removed in favor of EXR environment

        // this.scene.fog = new THREE.Fog(0xC8E6C9, 20, 100);  // Disable fog to debug background

        // Camera
        this.camera = new THREE.PerspectiveCamera(
            50,
            window.innerWidth / window.innerHeight,
            0.1,
            1000
        );
        this.camera.position.set(8, 6, 8);

        // Renderer with Sketchfab-quality settings
        this.renderer = new THREE.WebGLRenderer({
            antialias: true,
            alpha: false,
            powerPreference: "high-performance",
            logarithmicDepthBuffer: false,  // Désactivé pour de meilleures performances
            precision: "highp",
            stencil: true,  // Activé pour post-processing avancé
            depth: true
        });
        this.renderer.setSize(window.innerWidth, window.innerHeight);
        this.renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));

        // Premium shadow settings - Configuration exacte Sketchfab
        this.renderer.shadowMap.enabled = true;
        this.renderer.shadowMap.type = THREE.PCFSoftShadowMap;
        this.renderer.shadowMap.autoUpdate = true;

        // Advanced tone mapping for photorealistic lighting - Sketchfab style
        this.renderer.toneMapping = THREE.ACESFilmicToneMapping;
        this.renderer.toneMappingExposure = 0.8;  // Reduced from 1.2 for more natural, less saturated look

        // Premium color rendering - Configuration Sketchfab
        this.renderer.outputColorSpace = THREE.SRGBColorSpace;

        // Activer le gamma correction (important pour Sketchfab quality)
        this.renderer.useLegacyLights = false; // Mode moderne Three.js (remplace physicallyCorrectLights)

        container.appendChild(this.renderer.domElement);

        // Enhanced lighting
        this.setupLighting();

        // Environment map
        this.setupEnvironment();

        // Ground with better material
        // this.createGround();

        // Add trees for realism
        // this.createTrees();

        // OrbitControls
        this.setupControls();

        // Raycaster for interactions
        this.raycaster = new THREE.Raycaster();
        this.mouse = new THREE.Vector2();

        // Create sensors
        this.createSensors();

        // Load GLB model
        this.loadModel();

        // Setup post-processing
        this.setupPostProcessing();

        // Event listeners
        window.addEventListener('click', (event) => this.onMouseClick(event), false);
        this.setupEventListeners();

        this.animate();
        window.addEventListener('resize', () => this.onWindowResize());
    }

    setupEnvironment() {
        // Load the background texture for environment mapping
        const pmremGenerator = new THREE.PMREMGenerator(this.renderer);
        pmremGenerator.compileEquirectangularShader();

        new EXRLoader().load(
            'assets/background.exr',
            (texture) => {
                texture.mapping = THREE.EquirectangularReflectionMapping;

                // Set background directly
                this.scene.background = texture;

                // Convert texture to equirectangular environment map for PBR
                const envMap = pmremGenerator.fromEquirectangular(texture).texture;
                this.scene.environment = envMap;

                // Update all materials that were already loaded
                this.scene.traverse((child) => {
                    if (child.isMesh && child.material) {
                        child.material.envMap = envMap;
                        child.material.needsUpdate = true;
                    }
                });

                texture.dispose();
                pmremGenerator.dispose();
                console.log('EXR Background loaded');
            },
            undefined,
            (error) => {
                console.warn('Could not load EXR background:', error);
                // Fallback to gradient
                this.createGradientSkybox();
                // Fallback environment
                this.scene.environment = pmremGenerator.fromScene(new THREE.Scene()).texture;
            }
        );
    }

    createGradientSkybox() {
        // Fallback gradient skybox
        const skyGeo = new THREE.SphereGeometry(500, 32, 15);
        const vertexShader = `
            varying vec3 vWorldPosition;
            void main() {
                vec4 worldPosition = modelMatrix * vec4(position, 1.0);
                vWorldPosition = worldPosition.xyz;
                gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
            }
        `;
        const fragmentShader = `
            uniform vec3 topColor;
            uniform vec3 bottomColor;
            uniform float offset;
            uniform float exponent;
            varying vec3 vWorldPosition;
            void main() {
                float h = normalize(vWorldPosition + offset).y;
                gl_FragColor = vec4(mix(bottomColor, topColor, max(pow(max(h, 0.0), exponent), 0.0)), 1.0);
            }
        `;
        const skyMat = new THREE.ShaderMaterial({
            uniforms: {
                topColor: { value: new THREE.Color(0x87CEEB) },
                bottomColor: { value: new THREE.Color(0xC8E6C9) },
                offset: { value: 33 },
                exponent: { value: 0.6 }
            },
            vertexShader: vertexShader,
            fragmentShader: fragmentShader,
            side: THREE.BackSide
        });
        const sky = new THREE.Mesh(skyGeo, skyMat);
        this.scene.add(sky);
    }

    setupLighting() {
        // Ambient light with subtle ambient occlusion simulation
        const ambientLight = new THREE.AmbientLight(0xffffff, 0.5);
        this.scene.add(ambientLight);

        // Main directional light (Sun) with ultra-high quality shadows - Sketchfab config
        const mainLight = new THREE.DirectionalLight(0xffffff, 2.0);  // Reduced from 3.5 for softer lighting
        mainLight.position.set(20, 40, 20);  // Position optimisée
        mainLight.castShadow = true;

        // Configuration ombres ultra-qualité Sketchfab
        mainLight.shadow.mapSize.width = 4096;   // Résolution maximale
        mainLight.shadow.mapSize.height = 4096;
        mainLight.shadow.camera.near = 0.5;
        mainLight.shadow.camera.far = 200;
        mainLight.shadow.camera.left = -50;
        mainLight.shadow.camera.right = 50;
        mainLight.shadow.camera.top = 50;
        mainLight.shadow.camera.bottom = -50;
        mainLight.shadow.bias = -0.0001;          // Optimisé pour éviter shadow acne
        mainLight.shadow.normalBias = 0.05;       // Augmenté pour de meilleurs résultats
        mainLight.shadow.radius = 3;              // Ombres plus douces
        this.scene.add(mainLight);

        // Secondary shadow-casting light for depth
        const secondaryLight = new THREE.DirectionalLight(0xb8d4ff, 1.5);
        secondaryLight.position.set(-20, 20, -15);
        secondaryLight.castShadow = true;
        secondaryLight.shadow.mapSize.width = 1024;
        secondaryLight.shadow.mapSize.height = 1024;
        secondaryLight.shadow.camera.near = 0.1;
        secondaryLight.shadow.camera.far = 100;
        secondaryLight.shadow.camera.left = -30;
        secondaryLight.shadow.camera.right = 30;
        secondaryLight.shadow.camera.top = 30;
        secondaryLight.shadow.camera.bottom = -30;
        secondaryLight.shadow.bias = -0.0001;
        this.scene.add(secondaryLight);

        // Rim light for depth and contour definition
        const rimLight = new THREE.DirectionalLight(0xffffff, 1.0);
        rimLight.position.set(0, 15, -25);
        this.scene.add(rimLight);

        // Hemisphere light for natural sky/ground lighting
        const hemiLight = new THREE.HemisphereLight(0x87ceeb, 0x6b8e23, 1.3);
        hemiLight.position.set(0, 50, 0);
        this.scene.add(hemiLight);

        // Premium accent lights with decay for realism
        const accentLight1 = new THREE.PointLight(0x9fefff, 2.0, 35, 2);
        accentLight1.position.set(12, 6, 12);
        accentLight1.castShadow = true;
        accentLight1.shadow.mapSize.width = 1024;
        accentLight1.shadow.mapSize.height = 1024;
        this.scene.add(accentLight1);

        const accentLight2 = new THREE.PointLight(0xffefbf, 1.5, 30, 2);
        accentLight2.position.set(-12, 5, -12);
        accentLight2.castShadow = true;
        accentLight2.shadow.mapSize.width = 1024;
        accentLight2.shadow.mapSize.height = 1024;
        this.scene.add(accentLight2);

        // Subtle ground bounce light
        const bounceLight = new THREE.PointLight(0x6b8e23, 0.8, 40, 2);
        bounceLight.position.set(0, 0.5, 0);
        this.scene.add(bounceLight);
    }

    createGround() {
        const groundGeometry = new THREE.PlaneGeometry(200, 200, 64, 64);

        // Generate procedural grass texture
        const grassTexture = this.generateGrassTexture();
        grassTexture.wrapS = THREE.RepeatWrapping;
        grassTexture.wrapT = THREE.RepeatWrapping;
        grassTexture.repeat.set(30, 30);
        grassTexture.anisotropy = 16;
        grassTexture.colorSpace = THREE.SRGBColorSpace;

        // Create grass-like material with texture
        const groundMaterial = new THREE.MeshStandardMaterial({
            map: grassTexture,
            color: 0xcccccc, // Neutral modulation
            roughness: 0.95,
            metalness: 0.0,
            bumpMap: grassTexture,
            bumpScale: 0.3
        });

        const ground = new THREE.Mesh(groundGeometry, groundMaterial);
        ground.rotation.x = -Math.PI / 2;
        ground.receiveShadow = true;

        // Add uneven terrain
        const positions = ground.geometry.attributes.position;
        for (let i = 0; i < positions.count; i++) {
            const z = positions.getZ(i);
            // Smoother, larger rolling hills
            const x = positions.getX(i);
            const y = positions.getY(i); // This is actually Z in world space before rotation
            const noise = Math.sin(x * 0.1) * Math.cos(y * 0.1) * 0.5 + Math.random() * 0.1;
            positions.setZ(i, z + noise);
        }
        positions.needsUpdate = true;
        ground.geometry.computeVertexNormals();

        this.scene.add(ground);
    }

    generateGrassTexture() {
        const canvas = document.createElement('canvas');
        canvas.width = 512;
        canvas.height = 512;
        const context = canvas.getContext('2d');

        // Base green color
        context.fillStyle = '#3a5f0b';
        context.fillRect(0, 0, 512, 512);

        // Add noise for grass texture
        for (let i = 0; i < 60000; i++) {
            const x = Math.random() * 512;
            const y = Math.random() * 512;
            const w = 1 + Math.random() * 2;
            const h = 1 + Math.random() * 3;

            // Randomize grass blade colors
            const colors = ['#4a7c10', '#2d4c08', '#5d8c1c', '#33550a'];
            context.fillStyle = colors[Math.floor(Math.random() * colors.length)];
            context.globalAlpha = 0.8;
            context.fillRect(x, y, w, h);
        }

        // Add some dirt/earth patches
        for (let i = 0; i < 500; i++) {
            const x = Math.random() * 512;
            const y = Math.random() * 512;
            const radius = 1 + Math.random() * 3;
            context.fillStyle = '#5d4037';
            context.globalAlpha = 0.3;
            context.beginPath();
            context.arc(x, y, radius, 0, Math.PI * 2);
            context.fill();
        }

        const texture = new THREE.CanvasTexture(canvas);
        return texture;
    }

    createTrees() {
        // Tree positions arranged around the greenhouse (avoiding center)
        const treePositions = [
            { x: -15, z: -15, scale: 1.0 },
            { x: -18, z: 10, scale: 1.2 },
            { x: 20, z: -12, scale: 0.9 },
            { x: 15, z: 15, scale: 1.1 },
            { x: -25, z: 5, scale: 1.3 },
            { x: 25, z: -5, scale: 0.8 },
            { x: -10, z: 20, scale: 1.0 },
            { x: 12, z: -20, scale: 1.1 },
            { x: -20, z: -25, scale: 1.2 },
            { x: 18, z: 22, scale: 0.9 },
        ];

        treePositions.forEach(pos => {
            const tree = this.createTree(pos.scale);
            tree.position.set(pos.x, 0, pos.z);
            this.scene.add(tree);
        });
    }

    createTree(scale = 1.0) {
        const tree = new THREE.Group();

        // Trunk
        const trunkGeometry = new THREE.CylinderGeometry(0.3 * scale, 0.4 * scale, 3 * scale, 8);
        const trunkMaterial = new THREE.MeshStandardMaterial({
            color: 0x4a3728,
            roughness: 0.9,
            metalness: 0.0
        });
        const trunk = new THREE.Mesh(trunkGeometry, trunkMaterial);
        trunk.position.y = 1.5 * scale;
        trunk.castShadow = true;
        trunk.receiveShadow = true;
        tree.add(trunk);

        // Foliage - multiple spheres for realistic tree crown
        const foliageColors = [0x2d5016, 0x3a6b1f, 0x295c14];
        const foliagePositions = [
            { x: 0, y: 3.5, z: 0, r: 1.5 },
            { x: 0.5, y: 3.2, z: 0.5, r: 1.2 },
            { x: -0.5, y: 3.2, z: -0.5, r: 1.2 },
            { x: 0.7, y: 4.0, z: -0.3, r: 1.0 },
            { x: -0.3, y: 3.8, z: 0.7, r: 1.0 },
        ];

        foliagePositions.forEach((pos, i) => {
            const foliageGeometry = new THREE.SphereGeometry(pos.r * scale, 8, 8);
            const foliageMaterial = new THREE.MeshStandardMaterial({
                color: foliageColors[i % foliageColors.length],
                roughness: 0.8,
                metalness: 0.0
            });
            const foliage = new THREE.Mesh(foliageGeometry, foliageMaterial);
            foliage.position.set(pos.x * scale, pos.y * scale, pos.z * scale);
            foliage.castShadow = true;
            foliage.receiveShadow = true;
            tree.add(foliage);
        });

        return tree;
    }

    setupControls() {
        this.controls = new OrbitControls(this.camera, this.renderer.domElement);
        this.controls.enableDamping = true;
        this.controls.dampingFactor = 0.05;
        this.controls.autoRotate = true;
        this.controls.autoRotateSpeed = 0.8;
        this.controls.minDistance = 3;
        this.controls.maxDistance = 50;
        this.controls.maxPolarAngle = Math.PI / 2.1;
    }

    setupPostProcessing() {
        // Post-processing configuration Sketchfab-quality
        this.composer = new EffectComposer(this.renderer);

        // Render pass
        const renderPass = new RenderPass(this.scene, this.camera);
        this.composer.addPass(renderPass);

        // SSAO pour l'ambient occlusion réaliste (comme Sketchfab)
        const ssaoPass = new SSAOPass(this.scene, this.camera, window.innerWidth, window.innerHeight);
        ssaoPass.kernelRadius = 12;      // Rayon d'échantillonnage optimisé
        ssaoPass.minDistance = 0.005;    // Distance minimale ajustée
        ssaoPass.maxDistance = 0.15;     // Distance maximale étendue
        ssaoPass.output = SSAOPass.OUTPUT.Default;  // Mode de sortie
        this.composer.addPass(ssaoPass);

        // Bloom subtil pour les lumières (Sketchfab style)
        const bloomPass = new UnrealBloomPass(
            new THREE.Vector2(window.innerWidth, window.innerHeight),
            0.15,   // strength - Reduced from 0.4 for subtle glow
            0.4,   // radius - Reduced
            0.9    // threshold - Increased to limit bloom to very bright areas
        );
        this.composer.addPass(bloomPass);

        // Output pass pour le rendu final
        const outputPass = new OutputPass();
        this.composer.addPass(outputPass);
    }

    createSensors() {
        mvp.sensors.forEach(sensor => {
            const colorHex = mvp.toHex(sensor.color);

            // 1. Create glow effect (small transparent sphere)
            const glowGeometry = new THREE.SphereGeometry(0.06, 10, 10);
            const glowMaterial = new THREE.MeshBasicMaterial({
                color: sensor.color,
                transparent: true,
                opacity: 0.15,
                depthWrite: false,
                blending: THREE.AdditiveBlending // Fix for black appearance
            });
            const glow = new THREE.Mesh(glowGeometry, glowMaterial);
            glow.position.set(sensor.position.x, sensor.position.y, sensor.position.z);
            this.scene.add(glow);

            // 2. Create sprite with SVG icon
            this.createSpriteFromSVG(sensor, colorHex);

            this.sensorMeshes[sensor.id] = { glow, sensor };
        });
    }

    createSpriteFromSVG(sensor, colorHex) {
        const canvas = document.createElement('canvas');
        canvas.width = 128;
        canvas.height = 128;
        const ctx = canvas.getContext('2d');

        const svgString = `<?xml version="1.0" encoding="UTF-8"?>
            <svg width="128" height="128" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                <rect width="128" height="128" fill="transparent"/>
                ${mvp.getSVGIcon(sensor.svg, '#ffffff').replace('<svg', '<g').replace('</svg>', '</g>')}
            </svg>`;

        const svgBlob = new Blob([svgString], { type: 'image/svg+xml' });
        const url = URL.createObjectURL(svgBlob);

        const img = new Image();
        img.onload = () => {
            ctx.clearRect(0, 0, canvas.width, canvas.height);

            const scale = 0.5;
            const scaledWidth = canvas.width * scale;
            const scaledHeight = canvas.height * scale;
            const x = (canvas.width - scaledWidth) / 2;
            const y = (canvas.height - scaledHeight) / 2;

            // Draw background circle for better visibility
            ctx.fillStyle = 'rgba(0, 0, 0, 0.6)'; // Dark semi-transparent background
            ctx.beginPath();
            ctx.arc(canvas.width / 2, canvas.height / 2, canvas.width / 2, 0, 2 * Math.PI);
            ctx.fill();

            ctx.drawImage(img, x, y, scaledWidth, scaledHeight);

            const texture = new THREE.CanvasTexture(canvas);
            const spriteMaterial = new THREE.SpriteMaterial({
                map: texture,
                transparent: true,
                color: 0xffffff // White icon on dark background
            });

            const sprite = new THREE.Sprite(spriteMaterial);
            sprite.position.set(sensor.position.x, sensor.position.y, sensor.position.z);
            sprite.scale.set(0.2, 0.2, 0.2);
            sprite.userData = { sensorId: sensor.id };

            this.scene.add(sprite);

            if (this.sensorMeshes[sensor.id]) {
                this.sensorMeshes[sensor.id].sprite = sprite;
            } else {
                this.sensorMeshes[sensor.id] = { sprite, sensor };
            }

            URL.revokeObjectURL(url);
        };

        img.onerror = () => {
            console.error('Error loading SVG image for', sensor.id);
            URL.revokeObjectURL(url);
            this.createFallbackSprite(sensor);
        };

        img.src = url;
    }

    createFallbackSprite(sensor) {
        const canvas = document.createElement('canvas');
        canvas.width = 64;
        canvas.height = 64;
        const ctx = canvas.getContext('2d');

        ctx.fillStyle = mvp.toHex(sensor.color);
        ctx.beginPath();
        ctx.arc(32, 32, 16, 0, 2 * Math.PI);
        ctx.fill();

        ctx.fillStyle = '#ffffff';
        ctx.font = 'bold 12px Arial';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillText(sensor.id.substring(0, 3), 32, 32);

        const texture = new THREE.CanvasTexture(canvas);
        const material = new THREE.SpriteMaterial({
            map: texture,
            transparent: true
        });
        const sprite = new THREE.Sprite(material);
        sprite.position.set(sensor.position.x, sensor.position.y, sensor.position.z);
        sprite.scale.set(0.15, 0.15, 0.15);
        sprite.userData = { sensorId: sensor.id };

        this.scene.add(sprite);

        if (this.sensorMeshes[sensor.id]) {
            this.sensorMeshes[sensor.id].sprite = sprite;
        } else {
            this.sensorMeshes[sensor.id] = { sprite, sensor };
        }
    }

    loadModel() {
        const loader = new GLTFLoader();
        loader.load(
            'assets/greenhouse_park_fbx_free.glb',
            (gltf) => {
                this.model = gltf.scene;

                // Premium material and shadow settings (Sketchfab-quality)
                this.model.traverse((child) => {
                    if (child.isMesh) {
                        // Enable high-quality shadows
                        child.castShadow = true;
                        child.receiveShadow = true;

                        // Optimize geometry with tangents for normal mapping
                        if (child.geometry) {
                            child.geometry.computeVertexNormals();
                            if (child.geometry.attributes.uv && !child.geometry.attributes.tangent) {
                                child.geometry.computeTangents();
                            }
                        }

                        // Enhance materials with premium PBR properties
                        if (child.material) {
                            child.material.needsUpdate = true;

                            // Apply environment map
                            if (this.scene.environment) {
                                child.material.envMap = this.scene.environment;
                                child.material.envMapIntensity = 0.8; // Reduced from 1.2
                            }

                            // Enhanced PBR materials - Sketchfab configuration
                            if (child.material.type === 'MeshStandardMaterial' || child.material.type === 'MeshPhysicalMaterial') {
                                // Valeurs PBR optimisées comme Sketchfab
                                child.material.metalness = child.material.metalness !== undefined ? child.material.metalness : 0.0;
                                child.material.roughness = child.material.roughness !== undefined ? child.material.roughness : 0.8;

                                // Enable better shading
                                child.material.flatShading = false;

                                // Augmenter l'intensité de l'environment map
                                child.material.envMapIntensity = 1.0; // Reduced from 1.5

                                // Add clearcoat for glass/shiny surfaces (Sketchfab premium)
                                if (child.material.type === 'MeshPhysicalMaterial') {
                                    if (child.material.transmission > 0 || child.material.metalness > 0.5) {
                                        child.material.clearcoat = 0.3;
                                        child.material.clearcoatRoughness = 0.1;
                                    }
                                }
                            } else if (child.material.type === 'MeshBasicMaterial') {
                                // Convert basic materials to standard for better lighting
                                const oldMaterial = child.material;
                                child.material = new THREE.MeshStandardMaterial({
                                    color: oldMaterial.color,
                                    map: oldMaterial.map,
                                    metalness: 0.0,
                                    roughness: 0.8,
                                    envMap: this.scene.environment,
                                    envMapIntensity: 1.0 // Reduced from 1.5
                                });
                                child.material.needsUpdate = true;
                            }

                            // Enhanced textures with proper color space and filtering
                            const textureProperties = ['map', 'normalMap', 'roughnessMap', 'metalnessMap', 'aoMap', 'emissiveMap'];
                            textureProperties.forEach(prop => {
                                if (child.material[prop]) {
                                    child.material[prop].anisotropy = this.renderer.capabilities.getMaxAnisotropy();

                                    // Proper color space management (Sketchfab standard)
                                    if (prop === 'map' || prop === 'emissiveMap') {
                                        child.material[prop].colorSpace = THREE.SRGBColorSpace;
                                    } else {
                                        child.material[prop].colorSpace = THREE.NoColorSpace;
                                    }

                                    // Enable mipmaps for better quality
                                    child.material[prop].generateMipmaps = true;
                                    child.material[prop].minFilter = THREE.LinearMipmapLinearFilter;
                                    child.material[prop].magFilter = THREE.LinearFilter;
                                }
                            });

                            // Enhanced normal map intensity
                            if (child.material.normalMap) {
                                child.material.normalScale = child.material.normalScale || new THREE.Vector2(1, 1);
                            }
                        }
                    }
                });

                this.scene.add(this.model);

                // Auto-scale and center the model
                const box = new THREE.Box3().setFromObject(this.model);
                const size = box.getSize(new THREE.Vector3());

                const maxDim = Math.max(size.x, size.y, size.z);
                const scale = 4 / maxDim;
                this.model.scale.multiplyScalar(scale);

                const scaledBox = new THREE.Box3().setFromObject(this.model);
                const centerX = (scaledBox.max.x + scaledBox.min.x) / 2;
                const centerZ = (scaledBox.max.z + scaledBox.min.z) / 2;
                const minY = scaledBox.min.y;

                this.model.position.set(-centerX, -minY + 0.1, -centerZ);  // +0.1 to sit perfectly on top of grass bumps

                document.getElementById('loading').classList.add('hidden');
                document.getElementById('controls').classList.add('visible');

                // Display user name after model loads and controls are visible
                setTimeout(() => {
                    if (authManager.user && authManager.user.name) {
                        document.getElementById('userName').textContent = `Hi, ${authManager.user.name}!`;
                        console.log('User name displayed:', authManager.user.name);
                    } else {
                        console.warn('No user data available to display');
                    }
                }, 100);
            },
            undefined,
            (error) => {
                console.error('Error loading model:', error);
                document.getElementById('loading').classList.add('hidden');
                document.getElementById('controls').classList.add('visible');
            }
        );
    }

    setupEventListeners() {
        // Reset camera button
        document.getElementById('resetButton').addEventListener('click', () => {
            this.resetCamera();
        });

        // Rotate toggle button
        document.getElementById('rotateButton').addEventListener('click', () => {
            this.toggleAutoRotate();
        });

        // Sensor popup close
        document.getElementById('closeSensorButton').addEventListener('click', () => {
            mvp.closeSensorPopup();
        });

        // Show chart button
        document.getElementById('showChartButton').addEventListener('click', () => {
            mvp.showSensorChart();
        });

        // Chart navigation buttons
        document.getElementById('backToSensorButton').addEventListener('click', () => {
            mvp.closeChartPopup();
        });

        document.getElementById('closeChartButton').addEventListener('click', () => {
            mvp.closeChartPopup();
            mvp.closeSensorPopup();
        });

        // Time range buttons
        document.querySelectorAll('.time-btn').forEach(button => {
            button.addEventListener('click', function () {
                const timeRange = this.getAttribute('data-time-range');
                mvp.changeTimeRange(timeRange);

                document.querySelectorAll('.time-btn').forEach(btn => btn.classList.remove('active'));
                this.classList.add('active');
            });
        });

        // Logout button
        document.getElementById('logoutButton').addEventListener('click', () => {
            // Logout user
            authManager.logout();

            // Redirect to IAM login
            authManager.login();
        });
    }

    onMouseClick(event) {
        // Calculate mouse position in normalized device coordinates
        this.mouse.x = (event.clientX / window.innerWidth) * 2 - 1;
        this.mouse.y = - (event.clientY / window.innerHeight) * 2 + 1;

        this.raycaster.setFromCamera(this.mouse, this.camera);

        // Get all intersections
        const intersects = this.raycaster.intersectObjects(this.scene.children, true);

        // Filter intersections to find the closest valid sensor click
        for (let i = 0; i < intersects.length; i++) {
            const intersect = intersects[i];
            const object = intersect.object;

            if (object.userData && object.userData.sensorId) {
                // If it's a sprite (icon), check if we hit the visible circle
                if (object.isSprite) {
                    // Transform intersection point to sprite's local space
                    const localPoint = object.worldToLocal(intersect.point.clone());

                    // Sprite is 1x1 in local space (from -0.5 to 0.5)
                    // Check if point is within the circle (radius 0.5)
                    // We use 0.45 to be slightly stricter and avoid edge clicks
                    const dist = Math.sqrt(localPoint.x * localPoint.x + localPoint.y * localPoint.y);

                    if (dist > 0.45) {
                        continue; // Clicked on transparent corner, ignore
                    }
                }

                const sensorId = object.userData.sensorId;
                const sensor = mvp.sensors.find(s => s.id === sensorId);

                if (sensor) {
                    mvp.showSensorPopup(sensor);
                    return; // Stop after finding the first valid sensor
                }
            }
        }
    }

    animate() {
        requestAnimationFrame(() => this.animate());

        // Subtle glow animation
        const time = Date.now() * 0.001;
        Object.values(this.sensorMeshes).forEach((meshes, i) => {
            if (meshes.glow) {
                meshes.glow.scale.setScalar(1 + Math.sin(time * 1.5 + i) * 0.03);
            }
        });

        // Update actuator visualizations
        this.updateActuatorVisuals();

        // Update sensor visualizations based on thresholds
        this.updateSensorVisuals();

        this.controls.update();

        // Use composer for post-processing render
        if (this.composer) {
            this.composer.render();
        } else {
            this.renderer.render(this.scene, this.camera);
        }
    }

    updateSensorVisuals() {
        const actuatorIds = ['lamp1', 'lamp2', 'fan1', 'fan2', 'pump'];

        // Check each sensor against its thresholds
        mvp.sensors.forEach(sensor => {
            // Skip actuators as they are handled by updateActuatorVisuals
            if (actuatorIds.includes(sensor.id)) return;

            const meshData = this.sensorMeshes[sensor.id];
            if (!meshData || !meshData.sprite) return;

            const sensorData = mvp.sensorValues[sensor.id];
            if (!sensorData) return;

            const value = sensorData.current;
            let alertColor = null;

            // Check critical thresholds
            if (sensor.criticalHigh && value >= sensor.criticalHigh) {
                alertColor = 0xFF0000; // Red - Critical high
            } else if (sensor.criticalLow && value <= sensor.criticalLow) {
                alertColor = 0xFF0000; // Red - Critical low
            }
            // Check warning thresholds
            else if (sensor.warningHigh && value >= sensor.warningHigh) {
                alertColor = 0xFFA500; // Orange - Warning high
            } else if (sensor.warningLow && value <= sensor.warningLow) {
                alertColor = 0xFFA500; // Orange - Warning low
            }

            // Apply color to sprite
            if (meshData.sprite && meshData.sprite.material) {
                if (alertColor) {
                    meshData.sprite.material.color.setHex(alertColor);
                    // Make glow more visible for alerts
                    if (meshData.glow) {
                        meshData.glow.material.color.setHex(alertColor);
                        meshData.glow.material.opacity = 0.5;
                    }
                } else {
                    // Normal state - white sprite (on dark background)
                    meshData.sprite.material.color.setHex(0xFFFFFF);
                    // Reset glow to original color
                    if (meshData.glow) {
                        meshData.glow.material.color.setHex(sensor.color);
                        meshData.glow.material.opacity = 0.15;
                    }
                }
            }
        });
    }

    updateActuatorVisuals() {
        const actuatorIds = ['lamp1', 'lamp2', 'fan1', 'fan2', 'pump'];

        actuatorIds.forEach(id => {
            const meshData = this.sensorMeshes[id];
            if (!meshData) {
                console.warn(`No mesh data for ${id}`);
                return;
            }

            const sensorData = mvp.sensorValues[id];
            if (!sensorData || !sensorData.data) {
                console.warn(`No sensor data for ${id}`, sensorData);
                return;
            }

            // Get actuator state
            const isOn = sensorData.data.state === 'ON' || sensorData.data.lastCommand === 'ON';
            console.log(`Actuator ${id}: isOn=${isOn}, state=${sensorData.data.state}, lastCommand=${sensorData.data.lastCommand}`);

            // Special handling for bulbs (lamps)
            if (id === 'lamp1' || id === 'lamp2') {
                // Change glow color and intensity
                if (isOn) {
                    console.log(`🔆 Turning ${id} ON - Yellow glow`);
                    meshData.glow.material.color.setHex(0xFFFF00); // Bright yellow
                    meshData.glow.material.opacity = 0.9; // Very visible
                    meshData.glow.scale.setScalar(3.0); // Much larger

                    // Change sprite color to yellow too
                    if (meshData.sprite && meshData.sprite.material) {
                        meshData.sprite.material.color.setHex(0xFFFF00);
                    }

                    // Add point light if not exists
                    if (!meshData.light) {
                        const light = new THREE.PointLight(0xFFFF00, 5, 5); // Brighter and farther
                        light.position.copy(meshData.glow.position);
                        this.scene.add(light);
                        meshData.light = light;
                        console.log(`💡 Added point light to ${id}`);
                    }
                } else {
                    console.log(`🔅 Turning ${id} OFF - Original color`);
                    // Reset to original color
                    const sensor = meshData.sensor;
                    meshData.glow.material.color.setHex(sensor.color);
                    meshData.glow.material.opacity = 0.15;
                    meshData.glow.scale.setScalar(1.0);

                    // Reset sprite color to white
                    if (meshData.sprite && meshData.sprite.material) {
                        meshData.sprite.material.color.setHex(0xFFFFFF);
                    }

                    // Remove point light
                    if (meshData.light) {
                        this.scene.remove(meshData.light);
                        meshData.light = null;
                        console.log(`🔦 Removed point light from ${id}`);
                    }
                }
            }
            // Can add visual feedback for fans and pump too
            else if (id === 'fan1' || id === 'fan2') {
                if (isOn) {
                    meshData.glow.material.color.setHex(0x00D2FF); // Cyan for fans
                    meshData.glow.material.opacity = 0.4;

                    // Change sprite color to cyan
                    if (meshData.sprite && meshData.sprite.material) {
                        meshData.sprite.material.color.setHex(0x00D2FF);
                    }
                } else {
                    const sensor = meshData.sensor;
                    meshData.glow.material.color.setHex(sensor.color);
                    meshData.glow.material.opacity = 0.15;

                    // Reset sprite color to white
                    if (meshData.sprite && meshData.sprite.material) {
                        meshData.sprite.material.color.setHex(0xFFFFFF);
                    }
                }
            }
            else if (id === 'pump') {
                if (isOn) {
                    meshData.glow.material.color.setHex(0x5f27cd); // Purple for pump
                    meshData.glow.material.opacity = 0.4;

                    // Change sprite color to purple
                    if (meshData.sprite && meshData.sprite.material) {
                        meshData.sprite.material.color.setHex(0x5f27cd);
                    }
                } else {
                    const sensor = meshData.sensor;
                    meshData.glow.material.color.setHex(sensor.color);
                    meshData.glow.material.opacity = 0.15;

                    // Reset sprite color to white
                    if (meshData.sprite && meshData.sprite.material) {
                        meshData.sprite.material.color.setHex(0xFFFFFF);
                    }
                }
            }
        });
    }

    onWindowResize() {
        this.camera.aspect = window.innerWidth / window.innerHeight;
        this.camera.updateProjectionMatrix();
        this.renderer.setSize(window.innerWidth, window.innerHeight);

        if (this.composer) {
            this.composer.setSize(window.innerWidth, window.innerHeight);
        }
    }

    resetCamera() {
        this.camera.position.set(8, 6, 8);
        this.controls.target.set(0, 0, 0);
        this.controls.update();
    }

    toggleAutoRotate() {
        this.autoRotateEnabled = !this.autoRotateEnabled;
        this.controls.autoRotate = this.autoRotateEnabled;

        document.getElementById('rotateIcon').textContent = this.autoRotateEnabled ? '⏸' : '▶';
        document.getElementById('rotateText').textContent = this.autoRotateEnabled ? 'Pause' : 'Rotate';
    }
}

// Initialize application when page loads
document.addEventListener('DOMContentLoaded', () => {
    window.mainApp = new GreenhouseApp();
});

// Export for module use
export { GreenhouseApp };
