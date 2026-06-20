#version 330 core
in vec2 vTexCoord;
out vec4 fragColor;

uniform sampler2D uTextureLive;
uniform sampler2D uTextureHistory;

uniform float uDecay;
uniform float uGain;
uniform float uZoom;
uniform float uRotate;
uniform float uHueShift;
uniform float uBlur;

// RGB to HSV helper
vec3 rgb2hsv(vec3 c) {
    vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
    vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));

    float d = q.x - min(q.w, q.y);
    float e = 1.0e-10;
    return vec3(abs(q.z + (q.y - q.z) / (6.0 * d + e)), d / (q.x + e), q.x);
}

// HSV to RGB helper
vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

void main() {
    vec4 liveColor = texture(uTextureLive, vTexCoord);

    // Apply coordinate transformations around center (0.5, 0.5) for zoom/rotate feedback
    vec2 uv = vTexCoord - vec2(0.5);
    
    // Zoom factor (positive zooms in)
    uv *= (1.0 - uZoom);

    // Rotation factor (radians)
    float cosRot = cos(uRotate);
    float sinRot = sin(uRotate);
    uv = vec2(
        uv.x * cosRot - uv.y * sinRot,
        uv.x * sinRot + uv.y * cosRot
    );
    vec2 historyUV = uv + vec2(0.5);

    // Sample historical buffer with optional 5-tap box blur
    vec4 historyColor = vec4(0.0);
    if (uBlur > 0.0) {
        float offset = uBlur * 0.005;
        historyColor += texture(uTextureHistory, historyUV);
        historyColor += texture(uTextureHistory, historyUV + vec2(offset, 0.0));
        historyColor += texture(uTextureHistory, historyUV + vec2(-offset, 0.0));
        historyColor += texture(uTextureHistory, historyUV + vec2(0.0, offset));
        historyColor += texture(uTextureHistory, historyUV + vec2(0.0, -offset));
        historyColor /= 5.0;
    } else {
        historyColor = texture(uTextureHistory, historyUV);
    }

    // Apply decay and gain
    historyColor.rgb *= uGain;
    historyColor.a = clamp(historyColor.a - uDecay, 0.0, 1.0);

    // Apply hue shift to history
    if (uHueShift != 0.0 && historyColor.a > 0.0) {
        vec3 hsv = rgb2hsv(historyColor.rgb);
        hsv.x = fract(hsv.x + uHueShift);
        historyColor.rgb = hsv2rgb(hsv);
    }

    // Blend live frame with history (screen/maximum blending works best for trails)
    vec4 blended = max(liveColor, historyColor * historyColor.a);
    fragColor = blended;
}
