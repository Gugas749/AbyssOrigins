#version 150

// "Field" material — energy membranes (domes, shells, orbs).
// Vertex alpha already carries a fresnel rim from the CPU; this adds drifting cells,
// rising scan-lines and a fine lattice so surfaces read as solid arcane barriers.

uniform vec4 ColorModulator;
uniform float GameTime;
uniform float FogStart;
uniform float FogEnd;

in vec2 texCoord0;
in vec4 vertexColor;
in float vertexDistance;

out vec4 fragColor;

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float noise(vec2 p, float period) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    float x0 = mod(i.x, period), x1 = mod(i.x + 1.0, period);
    return mix(mix(hash(vec2(x0, i.y)), hash(vec2(x1, i.y)), u.x),
               mix(hash(vec2(x0, i.y + 1.0)), hash(vec2(x1, i.y + 1.0)), u.x), u.y);
}

float fbm(vec2 p, float period) {
    float v = 0.0, a = 0.5;
    for (int i = 0; i < 4; i++) {
        v += a * noise(p, period);
        p = p * 2.0 + vec2(0.0, 31.7);
        period *= 2.0;
        a *= 0.5;
    }
    return v;
}

float fogFade(float d) {
    if (d <= FogStart) return 1.0;
    if (d >= FogEnd) return 0.0;
    return smoothstep(FogEnd, FogStart, d);
}

void main() {
    float t = GameTime * 1200.0;
    vec2 uv = texCoord0;

    float n = fbm(vec2(uv.x * 2.0 + t * 0.25, uv.y * 5.0 - t * 0.45), 16.0);
    float cells = smoothstep(0.52, 0.78, n);
    float scan = pow(1.0 - abs(fract(uv.y * 5.0 - t * 0.6) * 2.0 - 1.0), 10.0);
    float lattice = pow(1.0 - abs(fract(uv.x * 4.0) * 2.0 - 1.0), 18.0)
                  + pow(1.0 - abs(fract(uv.y * 12.0) * 2.0 - 1.0), 18.0);

    float glow = 0.22 + n * 0.55 + cells * 0.75 + scan * 0.6 + lattice * 0.25;
    vec3 col = vertexColor.rgb * glow * 1.2;
    float a = clamp(vertexColor.a, 0.0, 1.0) * fogFade(vertexDistance);
    fragColor = vec4(col, a) * ColorModulator;
}
