// main.js - Main application logic for Three.js
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
        
        this.init();
    }

    init() {
        const container = document.getElementById('container');
        
        // Scene with better background
        this.scene = new THREE.Scene();
        this.scene.background = new THREE.Color(0x0f172a);
        this.scene.fog = new THREE.Fog(0x0f172a, 10, 50);

        // Camera
        this.camera = new THREE.PerspectiveCamera(
            45,
            window.innerWidth / window.innerHeight,
            0.1,
            1000
        );
        this.camera.position.set(8, 6, 8);

        // Renderer with better settings
        this.renderer = new THREE.WebGLRenderer({ 
            antialias: true,
            alpha: true,
            powerPreference: "high-performance"
        });
        this.renderer.setSize(window.innerWidth, window.innerHeight);
        this.renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
        this.renderer.shadowMap.enabled = true;
        this.renderer.shadowMap.type = THREE.PCFSoftShadowMap;
        this.renderer.toneMapping = THREE.ACESFilmicToneMapping;
        this.renderer.toneMappingExposure = 1.2;
        container.appendChild(this.renderer.domElement);

        // Enhanced lighting
        this.setupLighting();

        // Ground with better material
        this.createGround();

        // OrbitControls
        this.setupControls();

        // Raycaster for interactions
        this.raycaster = new THREE.Raycaster();
        this.mouse = new THREE.Vector2();

        // Create sensors
        this.createSensors();

        // Load GLB model
        this.loadModel();

        // Event listeners
        window.addEventListener('click', (event) => this.onMouseClick(event), false);
        this.setupEventListeners();

        this.animate();
        window.addEventListener('resize', () => this.onWindowResize());
    }

    setupLighting() {
        // Enhanced ambient light
        const ambientLight = new THREE.AmbientLight(0xffffff, 0.8);
        this.scene.add(ambientLight);

        // Main directional light with better settings
        const mainLight = new THREE.DirectionalLight(0xffffff, 1.5);
        mainLight.position.set(10, 15, 10);
        mainLight.castShadow = true;
        mainLight.shadow.mapSize.width = 2048;
        mainLight.shadow.mapSize.height = 2048;
        mainLight.shadow.camera.near = 0.5;
        mainLight.shadow.camera.far = 50;
        mainLight.shadow.camera.left = -20;
        mainLight.shadow.camera.right = 20;
        mainLight.shadow.camera.top = 20;
        mainLight.shadow.camera.bottom = -20;
        this.scene.add(mainLight);

        // Fill light
        const fillLight = new THREE.DirectionalLight(0xffffff, 0.4);
        fillLight.position.set(-8, 8, -8);
        this.scene.add(fillLight);

        // Back light
        const backLight = new THREE.DirectionalLight(0xffffff, 0.3);
        backLight.position.set(0, 5, -15);
        this.scene.add(backLight);

        // Hemisphere light for more natural lighting
        const hemiLight = new THREE.HemisphereLight(0xffffff, 0x444444, 0.6);
        hemiLight.position.set(0, 20, 0);
        this.scene.add(hemiLight);

        // Point lights for additional illumination
        const pointLight1 = new THREE.PointLight(0x4facfe, 0.3, 20);
        pointLight1.position.set(5, 3, 5);
        this.scene.add(pointLight1);

        const pointLight2 = new THREE.PointLight(0x00c6fb, 0.2, 20);
        pointLight2.position.set(-5, 3, -5);
        this.scene.add(pointLight2);
    }

    createGround() {
        const groundGeometry = new THREE.PlaneGeometry(100, 100);
        const groundMaterial = new THREE.MeshLambertMaterial({ 
            color: 0x1e293b,
            transparent: true,
            opacity: 0.8
        });
        const ground = new THREE.Mesh(groundGeometry, groundMaterial);
        ground.rotation.x = -Math.PI / 2;
        ground.receiveShadow = true;
        this.scene.add(ground);

        // Add grid helper for better spatial reference
        const gridHelper = new THREE.GridHelper(100, 20, 0x334155, 0x334155);
        gridHelper.position.y = 0.01;
        this.scene.add(gridHelper);
    }

    setupControls() {
        this.controls = new THREE.OrbitControls(this.camera, this.renderer.domElement);
        this.controls.enableDamping = true;
        this.controls.dampingFactor = 0.05;
        this.controls.autoRotate = true;
        this.controls.autoRotateSpeed = 1.0;
        this.controls.minDistance = 3;
        this.controls.maxDistance = 50;
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
                depthWrite: false
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
        
        const svgBlob = new Blob([svgString], {type: 'image/svg+xml'});
        const url = URL.createObjectURL(svgBlob);
        
        const img = new Image();
        img.onload = () => {
            ctx.clearRect(0, 0, canvas.width, canvas.height);
            
            const scale = 0.5;
            const scaledWidth = canvas.width * scale;
            const scaledHeight = canvas.height * scale;
            const x = (canvas.width - scaledWidth) / 2;
            const y = (canvas.height - scaledHeight) / 2;
            
            ctx.drawImage(img, x, y, scaledWidth, scaledHeight);
            
            const texture = new THREE.CanvasTexture(canvas);
            const spriteMaterial = new THREE.SpriteMaterial({ 
                map: texture,
                transparent: true,
                color: 0xffffff
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
        const loader = new THREE.GLTFLoader();
        loader.load(
            'assets/greenhouse_park_fbx_free.glb',
            (gltf) => {
                this.model = gltf.scene;
                
                this.model.traverse((child) => {
                    if (child.isMesh) {
                        child.castShadow = true;
                        child.receiveShadow = true;
                    }
                });

                this.scene.add(this.model);

                const box = new THREE.Box3().setFromObject(this.model);
                const size = box.getSize(new THREE.Vector3());
                
                const maxDim = Math.max(size.x, size.y, size.z);
                const scale = 4 / maxDim;
                this.model.scale.multiplyScalar(scale);
                
                const scaledBox = new THREE.Box3().setFromObject(this.model);
                const centerX = (scaledBox.max.x + scaledBox.min.x) / 2;
                const centerZ = (scaledBox.max.z + scaledBox.min.z) / 2;
                const minY = scaledBox.min.y;
                
                this.model.position.set(-centerX, -minY, -centerZ);

                document.getElementById('loading').classList.add('hidden');
                document.getElementById('controls').classList.add('visible');
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
            button.addEventListener('click', function() {
                const timeRange = this.getAttribute('data-time-range');
                mvp.changeTimeRange(timeRange);
                
                document.querySelectorAll('.time-btn').forEach(btn => btn.classList.remove('active'));
                this.classList.add('active');
            });
        });
    }

    onMouseClick(event) {
        this.mouse.x = (event.clientX / window.innerWidth) * 2 - 1;
        this.mouse.y = - (event.clientY / window.innerHeight) * 2 + 1;
        
        this.raycaster.setFromCamera(this.mouse, this.camera);
        const intersects = this.raycaster.intersectObjects(this.scene.children, true);
        
        for (let i = 0; i < intersects.length; i++) {
            const object = intersects[i].object;
            
            if (object.userData && object.userData.sensorId) {
                const sensorId = object.userData.sensorId;
                const sensor = mvp.sensors.find(s => s.id === sensorId);
                
                if (sensor) {
                    mvp.showSensorPopup(sensor);
                    return;
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
        
        this.controls.update();
        this.renderer.render(this.scene, this.camera);
    }

    onWindowResize() {
        this.camera.aspect = window.innerWidth / window.innerHeight;
        this.camera.updateProjectionMatrix();
        this.renderer.setSize(window.innerWidth, window.innerHeight);
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