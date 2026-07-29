#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
uniform float ShockwaveCenterX;
uniform float ShockwaveCenterY;
uniform float ShockwaveStrength;
uniform float ShockwavePhase;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    float aspect = InSize.x / max(InSize.y, 1.0);
    vec2 fromCenter = texCoord - vec2(ShockwaveCenterX, ShockwaveCenterY);
    vec2 scaledDelta = vec2(fromCenter.x * aspect, fromCenter.y);
    float radialDistance = length(scaledDelta);
    vec2 radialDirection = radialDistance > 0.0001
        ? scaledDelta / radialDistance
        : vec2(0.0);

    float ripple = sin(radialDistance * 72.0 - ShockwavePhase * 6.2831853);
    ripple += 0.45 * sin(radialDistance * 131.0 + ShockwavePhase * 9.424778);
    float edgeFade = smoothstep(0.015, 0.12, radialDistance)
        * (1.0 - smoothstep(0.85, 1.45, radialDistance));
    float displacement = ShockwaveStrength * edgeFade * (0.0070 + ripple * 0.0035);
    vec2 uvOffset = radialDirection * displacement;
    uvOffset.x /= aspect;

    vec2 refractedUv = clamp(texCoord + uvOffset, vec2(0.001), vec2(0.999));
    vec4 scene = texture(DiffuseSampler, refractedUv);
    float flashShape = 1.0 - smoothstep(0.0, 1.2, radialDistance);
    vec3 flashColor = vec3(1.0, 0.66, 0.34) * ShockwaveStrength * flashShape * 0.16;
    // The following minecraft:blit pass uses source-alpha blending after its
    // output target has been cleared. The scene target's alpha is not a stable
    // opacity value, so forwarding it can make the cleared frame stay blank.
    fragColor = vec4(min(scene.rgb + flashColor, vec3(1.0)), 1.0);
}
