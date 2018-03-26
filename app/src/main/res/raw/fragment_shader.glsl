uniform sampler2D sourceImage;
uniform sampler2D curvesImage;


varying highp vec2 texCoord;





//
//
                highp vec3 rgbToHsl(highp vec3 color) {
                    highp vec3 hsl;
                    highp float fmin = min(min(color.r, color.g), color.b);
                    highp float fmax = max(max(color.r, color.g), color.b);
                    highp float delta = fmax - fmin;
                    hsl.z = (fmax + fmin) / 2.0;
                    if (delta == 0.0) {
                        hsl.x = 0.0;
                        hsl.y = 0.0;
                    } else {
                        if (hsl.z < 0.5) {
                            hsl.y = delta / (fmax + fmin);
                        } else {
                            hsl.y = delta / (2.0 - fmax - fmin);
                        }
                        highp float deltaR = (((fmax - color.r) / 6.0) + (delta / 2.0)) / delta;
                        highp float deltaG = (((fmax - color.g) / 6.0) + (delta / 2.0)) / delta;
                        highp float deltaB = (((fmax - color.b) / 6.0) + (delta / 2.0)) / delta;
                        if (color.r == fmax) {
                            hsl.x = deltaB - deltaG;
                        } else if (color.g == fmax) {
                            hsl.x = (1.0 / 3.0) + deltaR - deltaB;
                        } else if (color.b == fmax) {
                            hsl.x = (2.0 / 3.0) + deltaG - deltaR;
                        }
                        if (hsl.x < 0.0) {
                            hsl.x += 1.0;
                        } else if (hsl.x > 1.0) {
                            hsl.x -= 1.0;
                        }
                    }
                    return hsl;
                }
//
//
                highp float hueToRgb(highp float f1, highp float f2, highp float hue) {
                                                                    if (hue < 0.0) {
                                                                        hue += 1.0;
                                                                    } else if (hue > 1.0) {
                                                                        hue -= 1.0;
                                                                    }
                                                                    highp float res;
                                                                    if ((6.0 * hue) < 1.0) {
                                                                        res = f1 + (f2 - f1) * 6.0 * hue;
                                                                    } else if ((2.0 * hue) < 1.0) {
                                                                        res = f2;
                                                                    } else if ((3.0 * hue) < 2.0) {
                                                                        res = f1 + (f2 - f1) * ((2.0 / 3.0) - hue) * 6.0;
                                                                    } else {
                                                                        res = f1;
                                                                    } return res;
                                                                }

                highp vec3 hslToRgb(highp vec3 hsl) {
                                    if (hsl.y == 0.0) {
                                        return vec3(hsl.z);
                                    } else {
                                        highp float f2;
                                        if (hsl.z < 0.5) {
                                            f2 = hsl.z * (1.0 + hsl.y);
                                        } else {
                                            f2 = (hsl.z + hsl.y) - (hsl.y * hsl.z);
                                        }
                                        highp float f1 = 2.0 * hsl.z - f2;
                                        return vec3(hueToRgb(f1, f2, hsl.x + (1.0/3.0)), hueToRgb(f1, f2, hsl.x), hueToRgb(f1, f2, hsl.x - (1.0/3.0)));

                                    }
                                }


                lowp vec3 applyLuminanceCurve(lowp vec3 pixel) {
                    highp float index = floor(clamp(pixel.z / (1.0 / 200.0), 0.0, 199.0));
                    pixel.y = mix(0.0, pixel.y, smoothstep(0.0, 0.1, pixel.z) * (1.0 - smoothstep(0.8, 1.0, pixel.z)));
                    pixel.z = texture2D(curvesImage, vec2(1.0 / 200.0 * index, 0)).a;
                    return pixel;
                }

                lowp vec3 applyRGBCurve(lowp vec3 pixel) {
                                                   highp float index = floor(clamp(pixel.r / (1.0 / 200.0), 0.0, 199.0));
                                                    pixel.r = texture2D(curvesImage, vec2(1.0 / 200.0 * index, 0)).r;
                                                   index = floor(clamp(pixel.g / (1.0 / 200.0), 0.0, 199.0));
                                                    pixel.g = clamp(texture2D(curvesImage, vec2(1.0 / 200.0 * index, 0)).g, 0.0, 1.0);
                                                    index = floor(clamp(pixel.b / (1.0 / 200.0), 0.0, 199.0));
                                                    pixel.b = clamp(texture2D(curvesImage, vec2(1.0 / 200.0 * index, 0)).b, 0.0, 1.0);
                                                    return pixel;
                                                }








void main(void) {


lowp vec4 source = texture2D(sourceImage, texCoord);
                    lowp vec4 result = source;


                        result = vec4(applyRGBCurve(hslToRgb(applyLuminanceCurve(rgbToHsl(result.rgb)))), result.a);




                    gl_FragColor = result;



}