#version 330 core
in vec2 vTexCoord;
out vec4 fragColor;

uniform int uBgStyle; // 0 = Off, 1 = Solid, 2 = Plasma
uniform float uBgHue;
uniform float uBgSat;
uniform float uBgVal;
uniform float uBgSweep;
uniform float uBgSpeed;
uniform float uBgZoom;
uniform float uTime;
uniform float uAlpha;

// HSV to RGB helper
vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

void main() {
    if (uBgStyle <= 0) {
        fragColor = vec4(0.0, 0.0, 0.0, 0.0);
        return;
    }

    vec3 rgb = vec3(0.0);

    if (uBgStyle == 1) { // Solid Color
        rgb = hsv2rgb(vec3(uBgHue, uBgSat, uBgVal));
    } else if (uBgStyle == 2) { // Plasma
        float time = uTime * uBgSpeed * 2.0;
        
        // Center coordinates around (0,0) and scale by uBgZoom
        vec2 p = (vTexCoord - vec2(0.5)) * uBgZoom;
        
        float v1 = sin(p.x * 10.0 + time);
        float v2 = sin(10.0 * (p.x * sin(time / 2.0) + p.y * cos(time / 3.0)) + time);
        
        float cx = p.x + 0.5 * sin(time / 5.0);
        float cy = p.y + 0.5 * cos(time / 3.0);
        float v3 = sin(sqrt(100.0 * (cx * cx + cy * cy) + 1.0) + time);
        
        float val = (v1 + v2 + v3) / 3.0;
        float hue = fract(uBgHue + val * uBgSweep);
        rgb = hsv2rgb(vec3(hue, uBgSat, uBgVal));
    }

    fragColor = vec4(rgb, uAlpha);
}
