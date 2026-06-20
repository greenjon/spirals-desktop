#version 330 core
in vec2 vTexCoord;
out vec4 fragColor;

uniform sampler2D uTex1;
uniform sampler2D uTex2;
uniform int uMode; // 0 = ADD, 1 = SCREEN, 2 = MULT, 3 = MAX, 4 = XFADE
uniform float uBalance; // 0.0 = Tex1 (Deck A), 1.0 = Tex2 (Deck B)
uniform float uAlpha; // Master output alpha / gain

void main() {
    vec4 color1 = texture(uTex1, vTexCoord);
    vec4 color2 = texture(uTex2, vTexCoord);
    float t = uBalance;

    vec4 blended = vec4(0.0);

    if (uMode == 0) { // ADD
        blended = color1 * (1.0 - t) + color2 * t;
    } else if (uMode == 1) { // SCREEN
        vec4 c1 = color1 * (1.0 - t);
        vec4 c2 = color2 * t;
        blended = c1 + c2 - c1 * c2;
    } else if (uMode == 2) { // MULT
        vec4 mult = color1 * color2;
        if (t < 0.5) {
            blended = mix(color1, mult, t * 2.0);
        } else {
            blended = mix(mult, color2, (t - 0.5) * 2.0);
        }
    } else if (uMode == 3) { // MAX
        blended = max(color1 * (1.0 - t), color2 * t);
    } else { // XFADE (mode 4)
        blended = mix(color1, color2, t);
    }

    fragColor = blended * uAlpha;
}
