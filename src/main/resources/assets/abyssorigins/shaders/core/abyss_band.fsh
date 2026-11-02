#version 150

// "Band" material — living plasma ribbons (rings, beams, slashes, feathers).
//   UV.x : distance along the ribbon (noise is periodic every 8 units so closed rings have no seam)
//   UV.y : 0..1 across the ribbon; edges dissolve into licking flame, the centre burns white-hot
// Drawn with additive blending: rgb is light added to the scene, alpha is the overall fade.

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

// Value noise, periodic in x with the given period
float noise(vec2 p, float period) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    float x0 = mod(i.x, period), x1 = mod(i.x + 1.0, period);
    float a = hash(vec2(x0, i.y)), b = hash(vec2(x1, i.y));
    float c = hash(vec2(x0, i.y + 1.0)), d = hash(vec2(x1, i.y + 1.0));
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float fbm(vec2 p, float period) {
    float v = 0.0, a = 0.5;
    for (int i = 0; i < 4; i++) {
        v += a * noise(p, period);
        p = p * 2.0 + vec2(0.0, 17.13);
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
    float t = GameTime * 1200.0;               // seconds
    vec2 uv = texCoord0;
    float across = abs(uv.y * 2.0 - 1.0);       // 0 centre, 1 edge

    float flow  = fbm(vec2(uv.x * 1.0 - t * 0.9, uv.y * 1.6 + t * 0.2), 8.0);
    float flick = fbm(vec2(uv.x * 2.0 + t * 1.7, uv.y * 3.0 - t * 0.6), 16.0);

    float body = smoothstep(1.0, 0.12, across + (flow - 0.5) * 1.1);
    float core = pow(max(0.0, 1.0 - across), 5.0);
    float glow = body * (0.45 + 0.95 * flick) + core * 0.85;

    vec3 col = vertexColor.rgb * glow * 1.25 + vec3(1.0, 0.97, 0.92) * core * core * 0.7;
    float a = clamp(vertexColor.a, 0.0, 1.0) * fogFade(vertexDistance);
    fragColor = vec4(col, a) * ColorModulator;
}
