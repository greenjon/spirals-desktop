#version 330 core
layout (location = 0) in vec2 aPhaseSide; // x = phase (0..1), y = side (-1 or 1)

uniform float uL1;
uniform float uL2;
uniform float uL3;
uniform float uL4;
uniform float uA;
uniform float uB;
uniform float uC;
uniform float uD;

uniform float uThickness;
uniform float uGlobalScale;
uniform float uGlobalRotation;
uniform float uAspectRatio;

out float vPhase;
out vec2 vCurvePos;

const float PI = 3.14159265359;

void main() {
    float phase = aPhaseSide.x;
    float side = aPhaseSide.y;
    vPhase = phase;

    float t = phase * 2.0 * PI;

    // 1. Calculate position on the curve (x, y)
    float x = uL1 * cos(t * uA) + uL2 * cos(t * uB) + uL3 * cos(t * uC) + uL4 * cos(t * uD);
    float y = uL1 * sin(t * uA) + uL2 * sin(t * uB) + uL3 * sin(t * uC) + uL4 * sin(t * uD);
    vec2 pos = vec2(x, y);
    vCurvePos = pos; // Pass unscaled position to fragment shader for depth evaluation

    // 2. Calculate the derivative (tangent) analytically to find the normal vector
    float dx = -uA * uL1 * sin(t * uA) - uB * uL2 * sin(t * uB) - uC * uL3 * sin(t * uC) - uD * uL4 * sin(t * uD);
    float dy =  uA * uL1 * cos(t * uA) + uB * uL2 * cos(t * uB) + uC * uL3 * cos(t * uC) + uD * uL4 * cos(t * uD);
    vec2 tangent = vec2(dx, dy);

    // Normal is perpendicular to the tangent
    vec2 normal = vec2(-tangent.y, tangent.x);
    if (length(normal) > 0.0001) {
        normal = normalize(normal);
    } else {
        normal = vec2(0.0);
    }

    // Offset position by side and thickness to construct ribbon geometry
    pos += normal * (side * uThickness * 0.5);

    // 3. Apply global rotation
    float cosRot = cos(uGlobalRotation);
    float sinRot = sin(uGlobalRotation);
    vec2 rotatedPos = vec2(
        pos.x * cosRot - pos.y * sinRot,
        pos.x * sinRot + pos.y * cosRot
    );

    // 4. Apply scale
    rotatedPos *= uGlobalScale;

    // 5. Correct for screen aspect ratio to maintain a perfect square aspect ratio
    rotatedPos.x /= uAspectRatio;

    gl_Position = vec4(rotatedPos, 0.0, 1.0);
}
