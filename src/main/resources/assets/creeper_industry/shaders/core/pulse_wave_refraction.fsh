#version 150

uniform sampler2D SceneColor;
uniform sampler2D SceneDepth;
uniform mat4 ViewProjection;
uniform mat4 InverseViewProjection;
uniform vec2 ViewportSize;
uniform float ImpactStrength;
uniform float ImpactAge;
uniform float ImpactPolarity;
uniform int WaveCount;
uniform vec4 Wave0;
uniform vec4 Wave1;
uniform vec4 Wave2;
uniform vec4 Wave3;
uniform vec4 Wave4;
uniform vec4 Wave5;
uniform vec4 Wave6;
uniform vec4 Wave7;
uniform vec4 Style0;
uniform vec4 Style1;
uniform vec4 Style2;
uniform vec4 Style3;
uniform vec4 Style4;
uniform vec4 Style5;
uniform vec4 Style6;
uniform vec4 Style7;

in vec2 texCoord;
out vec4 fragColor;

vec3 unproject(vec2 uv, float depth) {
    vec4 position = InverseViewProjection * vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    return position.xyz / position.w;
}

// The ball is a union of unit cubes whose centers lie within radius + 0.5.
bool blockBallHit(vec3 relative, vec3 ray, float radius, float sceneDistance,
        out float hitDistance, out vec3 normal) {
    float ballRadius = radius + 0.5;
    float outerRadius = ballRadius + sqrt(0.75) + 0.001;
    float b = dot(relative, ray);
    float discriminant = b * b - dot(relative, relative) + outerRadius * outerRadius;
    if (discriminant <= 0.0) return false;
    float root = sqrt(discriminant);
    float travel = max(0.0, -b - root);
    float end = min(sceneDistance, -b + root);
    if (travel >= end) return false;

    float innerRadius = max(0.0, ballRadius - sqrt(0.75) - 0.001);
    float innerDiscriminant = b * b - dot(relative, relative) + innerRadius * innerRadius;
    float innerEntry = -b - sqrt(max(0.0, innerDiscriminant));
    float innerExit = -b + sqrt(max(0.0, innerDiscriminant));
    vec3 direction = vec3(ray.x >= 0.0 ? 1.0 : -1.0,
            ray.y >= 0.0 ? 1.0 : -1.0, ray.z >= 0.0 ? 1.0 : -1.0);
    vec3 inverseRay = direction / max(abs(ray), vec3(1e-8));
    vec3 delta = abs(inverseRay);
    vec3 cell = floor(relative + ray * (travel + 0.001) + 0.5);
    vec3 next = (cell + direction * 0.5 - relative) * inverseRay;
    next = mix(next, vec3(1e30), lessThan(abs(ray), vec3(1e-8)));
    bool inside = dot(cell, cell) <= ballRadius * ballRadius;

    while (travel < end) {
        // Traverse the surface band; the inner sphere contains only occupied cells.
        if (inside && innerDiscriminant > 0.0 && travel >= innerEntry && travel < innerExit) {
            travel = innerExit;
            if (travel >= end) break;
            cell = floor(relative + ray * (travel + 0.001) + 0.5);
            next = (cell + direction * 0.5 - relative) * inverseRay;
            next = mix(next, vec3(1e30), lessThan(abs(ray), vec3(1e-8)));
        }
        float crossing = min(next.x, min(next.y, next.z));
        if (crossing >= end) break;
        vec3 axes = step(next, vec3(crossing + 1e-5));
        cell += direction * axes;
        bool nextInside = dot(cell, cell) <= ballRadius * ballRadius;
        if (nextInside != inside) {
            hitDistance = crossing;
            normal = normalize(direction * axes) * (nextInside ? -1.0 : 1.0);
            return true;
        }
        next += delta * axes;
        travel = crossing;
    }
    return false;
}

void main() {
    vec4 original = texture(SceneColor, texCoord);
    float depth = texture(SceneDepth, texCoord).r;
    // Start rays at the near plane: this also handles view bob and hurt transforms
    // in the projection matrix, rather than assuming every ray starts at (0,0,0).
    vec3 rayStart = unproject(texCoord, 0.0);
    vec3 scenePosition = unproject(texCoord, depth);
    vec3 segment = scenePosition - rayStart;
    float sceneDistance = length(segment);
    vec3 ray = segment / max(sceneDistance, 0.0001);
    vec4 waves[8] = vec4[8](Wave0, Wave1, Wave2, Wave3, Wave4, Wave5, Wave6, Wave7);
    vec4 styles[8] = vec4[8](Style0, Style1, Style2, Style3, Style4, Style5, Style6, Style7);
    vec2 displacement = vec2(0.0);
    float rimLight = 0.0;
    float nearestSurface = sceneDistance;

    for (int i = 0; i < 8; i++) {
        if (i >= WaveCount) break;
        vec3 center = waves[i].xyz;
        float radius = waves[i].w;
        float strength = styles[i].x;
        float width = styles[i].y;
        vec3 relative = rayStart - center;
        float hitDistance;
        vec3 normal;
        if (!blockBallHit(relative, ray, radius, sceneDistance, hitDistance, normal)) continue;
        if (hitDistance <= 0.0) continue;

        vec3 hitPosition = rayStart + ray * hitDistance;
        float incidence = abs(dot(normal, ray));
        float rim = pow(1.0 - incidence, 2.0);
        float intersectionFade = smoothstep(0.0, width, sceneDistance - hitDistance);
        float nearFade = smoothstep(0.0, 0.12, hitDistance);
        float visibility = intersectionFade * nearFade * strength;

        // Project a tangent displacement at the actual surface; offscreen origins
        // and inside-sphere views need no special screen-centred fallback effect.
        vec3 tangent = normal - ray * dot(normal, ray);
        vec4 projectedHit = ViewProjection * vec4(hitPosition, 1.0);
        vec4 projectedOffset = ViewProjection * vec4(hitPosition + tangent * 0.1, 1.0);
        if (projectedHit.w <= 0.0001 || projectedOffset.w <= 0.0001) continue;
        vec2 direction = projectedOffset.xy / projectedOffset.w - projectedHit.xy / projectedHit.w;
        vec2 pixelDirection = direction * ViewportSize;
        pixelDirection /= max(length(pixelDirection), 0.0001);

        // Coherent modulation preserves the clear refractive surface.
        float ripple = sin(dot(normal, vec3(17.0, 23.0, 13.0)) + radius * 1.7 + styles[i].z);
        float pixels = (0.9 + 15.0 * rim) * (1.0 + 0.16 * ripple) * visibility * styles[i].w;
        displacement += pixelDirection * pixels;
        rimLight += rim * visibility * (styles[i].w > 0.0 ? 0.035 : -0.06);
        nearestSurface = min(nearestSurface, hitDistance);
    }

    // Multiple explosions share one copy/pass and cannot produce unbounded warp.
    displacement *= min(1.0, 20.0 / max(length(displacement), 0.0001));
    vec2 halfPixel = 0.5 / ViewportSize;
    vec2 displacedUv = clamp(texCoord + displacement / ViewportSize, halfPixel, 1.0 - halfPixel);

    // Do not pull nearby foreground geometry across a wave's depth boundary.
    float displacedDepth = texture(SceneDepth, displacedUv).r;
    vec3 displacedPosition = unproject(displacedUv, displacedDepth);
    if (dot(displacedPosition - rayStart, ray) + 0.08 < nearestSurface) {
        displacedUv = texCoord;
    }
    // A hit is a local visual response, including for a wave that arrived through a
    // wall. Apply it after the world-space occlusion check, with its own recovery.
    vec2 radial = (texCoord - 0.5) * vec2(ViewportSize.x / ViewportSize.y, 1.0);
    float radialLength = length(radial);
    vec2 impactDirection = radial / max(radialLength, 0.0001);
    float pressureRipple = sin(radialLength * 12.0 - ImpactAge * 1.8);
    vec2 impactWarp = impactDirection * (14.0 + 8.0 * pressureRipple)
            * smoothstep(0.0, 0.3, radialLength) * ImpactStrength * ImpactPolarity;
    displacedUv = clamp(displacedUv + impactWarp / ViewportSize, halfPixel, 1.0 - halfPixel);

    vec3 refracted = texture(SceneColor, displacedUv).rgb;
    if (ImpactStrength > 0.001) {
        // Nine fixed taps in the same pass; the extra samples run only during a hit.
        // A cross plus diagonals keeps the centre blurred as well as the periphery.
        vec2 blurStep = vec2(9.0 * ImpactStrength) / ViewportSize;
        vec3 blurred = refracted * 0.25;
        blurred += texture(SceneColor, clamp(displacedUv + vec2(blurStep.x, 0.0), halfPixel, 1.0 - halfPixel)).rgb * 0.125;
        blurred += texture(SceneColor, clamp(displacedUv - vec2(blurStep.x, 0.0), halfPixel, 1.0 - halfPixel)).rgb * 0.125;
        blurred += texture(SceneColor, clamp(displacedUv + vec2(0.0, blurStep.y), halfPixel, 1.0 - halfPixel)).rgb * 0.125;
        blurred += texture(SceneColor, clamp(displacedUv - vec2(0.0, blurStep.y), halfPixel, 1.0 - halfPixel)).rgb * 0.125;
        blurred += texture(SceneColor, clamp(displacedUv + blurStep, halfPixel, 1.0 - halfPixel)).rgb * 0.0625;
        blurred += texture(SceneColor, clamp(displacedUv - blurStep, halfPixel, 1.0 - halfPixel)).rgb * 0.0625;
        blurred += texture(SceneColor, clamp(displacedUv + vec2(blurStep.x, -blurStep.y), halfPixel, 1.0 - halfPixel)).rgb * 0.0625;
        blurred += texture(SceneColor, clamp(displacedUv + vec2(-blurStep.x, blurStep.y), halfPixel, 1.0 - halfPixel)).rgb * 0.0625;
        refracted = mix(refracted, blurred, min(1.0, ImpactStrength * 1.5));
    }
    fragColor = vec4(clamp(refracted + clamp(rimLight, -0.06, 0.06), 0.0, 1.0), original.a);
}
