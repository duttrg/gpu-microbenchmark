/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//package com.example.android.apis.graphics;
package com.example.gpu_microbenchmark;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

//import com.learnopengles.android.R;
import com.learnopengles.android.common.ShaderHelper;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;

class GLES20TriangleRenderer implements GLSurfaceView.Renderer {

    private long lastSeenTime;
    private Bitmap bitmap;
    private Bitmap bitmap0;
    private Bitmap bitmap1;
    private Bitmap bitmap2;
    private Bitmap bitmap3;
    private Bitmap bitmap4;
    private int textureIndex = 0;

    /*
     * Number of vertices to be rendered.
     */
    private int mNumVertices = 96 * 20;
    private int verMDynamicValue;
    private int appDynamicValue;
    private int verShDynamicValue;
    private int fragShDynamicValue;
    private int texMDynamicValue;


    /*
     * 0: No texture
     * 1: original (robot)
     * 2: 1 texture
     * 3: 2 kinds of texture alternate every N seconds
     * */
    private int textureMode=3;
    private int mFrameCount=0;

    private static final int SHADER_USE_POSITION = 1;
    private static final int SHADER_USE_COLOR = 1 << 1;
    private static final int SHADER_USE_NORMAL = 1 << 2;
    private static final int SHADER_USE_MVMATRIX = 1 << 3;
    private static final int SHADER_USE_MVPMATRIX = 1 << 4;
    private static final int SHADER_USE_LIGHT = 1 << 5;
    private static final int SHADER_USE_TEXTURE = 1 << 6;

    /*
     *  Shader mode: define the use of attribute
     */
    private static final int VS_ORIGIN = SHADER_USE_MVPMATRIX | SHADER_USE_POSITION | SHADER_USE_TEXTURE;
    private static final int VS_NONE = SHADER_USE_MVPMATRIX;
    private static final int VS_POSITION = SHADER_USE_POSITION | SHADER_USE_MVPMATRIX;
    private static final int VS_COLOR = VS_POSITION | SHADER_USE_COLOR;
    private static final int VS_LIGHT = VS_COLOR | SHADER_USE_LIGHT | SHADER_USE_MVMATRIX;
    private static final int VS_LIGHT2 = (1 << 31) | VS_COLOR | SHADER_USE_LIGHT | SHADER_USE_MVMATRIX;
    private static final int VS_TEXTURE = VS_LIGHT | SHADER_USE_TEXTURE;
    private int mVertexShaderMode = VS_ORIGIN;

    private static final int FS_ORIGIN = VS_ORIGIN;
    private static final int FS_NONE = 0;
    private static final int FS_COLOR = VS_COLOR;
    private static final int FS_LIGHT = VS_LIGHT;
    private static final int FS_TEXTURE = VS_TEXTURE;
    private int mFragmentShaderMode = FS_ORIGIN;

    private static volatile int dummy;
    private static volatile int dummy1;
    /**
     * Stores a copy of the model matrix specifically for the light position.
     */
    private float[] mLightModelMatrix = new float[16];
    /** Used to hold a light centered on the origin in model space. We need a 4th coordinate so we can get translations to work when
     *  we multiply this by our transformation matrices. */
    private final float[] mLightPosInModelSpace = new float[] {0.0f, 0.0f, 0.0f, 1.0f};

    /** Used to hold the current position of the light in world space (after transformation via model matrix). */
    private final float[] mLightPosInWorldSpace = new float[4];

    /** Used to hold the transformed position of the light in eye space (after transformation via modelview matrix) */
    private final float[] mLightPosInEyeSpace = new float[4];

    /** Identifiers for our uniforms and attributes inside the shaders. */
    private static final String MVP_MATRIX_UNIFORM = "uMVPMatrix";
    private static final String MV_MATRIX_UNIFORM = "uMVMatrix";
    private static final String POSITION_ATTRIBUTE = "aPosition";
    private static final String NORMAL_ATTRIBUTE = "aNormal";
    private static final String COLOR_ATTRIBUTE = "aColor";
    private static final String LIGHTPOS_ATTRIBUTE = "uLightPos";
    private static final String TEXTURE_ATTRIBUTE = "aTextureCoord";

    public GLES20TriangleRenderer(Activity activity, int verMDynamicValue, int appDynamicValue,
                                  int verShDynamicValue, int fragShDynamicValue, int texMDynamicValue) {
        mContext = activity;
        mActivity = activity;

        this.verMDynamicValue = verMDynamicValue;
        this.appDynamicValue = appDynamicValue;
        this.verShDynamicValue = verShDynamicValue;
        this.fragShDynamicValue = fragShDynamicValue;
        this.texMDynamicValue = texMDynamicValue;

        mTriangleVertices = ByteBuffer.allocateDirect(mTriangleVerticesData.length
                * FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTriangleVertices.put(mTriangleVerticesData).position(0);

        mTriangleColor = ByteBuffer.allocateDirect(mTriangleColorData.length
                * FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTriangleColor.put(mTriangleColorData).position(0);

        mTriangleNormal = ByteBuffer.allocateDirect(mTriangleNormalData.length
                * FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTriangleNormal.put(mTriangleNormalData).position(0);

        lastSeenTime = System.currentTimeMillis();
    }

    static int frameCount = 0;

    public void onDrawFrame(GL10 glUnused) {
        frameCount++;

        /* Simulate CPU busy working.  */
        for (dummy1 = 0; dummy1 < appDynamicValue; ++dummy1) {
            for (dummy = 0; dummy < 40000; ++dummy) {
            }
        }

        // Ignore the passed-in GL10 interface, and use the GLES20
        // class's static methods instead.
        GLES20.glClear( GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glUseProgram(mProgram);

        if((mFrameCount % 10) == 0){
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureID);
            if(texMDynamicValue == 256)
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap0, 0);
            else if(texMDynamicValue == 512)
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap1, 0);
            else if(texMDynamicValue == 1024)
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap2, 0);
            else if(texMDynamicValue == 2048)
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap3, 0);
            else if(texMDynamicValue == 4096) {
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap4, 0);
            }
        }

        if((frameCount % 150) == 0){
                mNumVertices = 24 * verMDynamicValue;
        }

        //GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureID);
        //GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap3, 0);

        if(mFrameCount >= 7200000){
            return;

        }else{

            if (textureMode == 4) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastSeenTime > 10000) {
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureID);
                    if (textureIndex == 0) {
                        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap1, 0);
                        textureIndex = 1;
                    } else {
                        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
                        textureIndex = 0;
                    }
                    lastSeenTime = currentTime;
                }
            }

            if ((mVertexShaderMode & SHADER_USE_POSITION) > 0) {
                // Pass in the position information
                mTriangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET);
                GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false,
                        TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices);
                GLES20.glEnableVertexAttribArray(maPositionHandle);
            }

            if ((mVertexShaderMode & SHADER_USE_COLOR) > 0) {
                // Pass in the color information
                mTriangleColor.position(0);
                GLES20.glVertexAttribPointer(maColorHandle, 4, GLES20.GL_FLOAT, false,
                        0, mTriangleColor);
                GLES20.glEnableVertexAttribArray(maColorHandle);
            }

            if ((mVertexShaderMode & SHADER_USE_NORMAL) > 0) {
                // Pass in the normal information
                mTriangleNormal.position(0);
                GLES20.glVertexAttribPointer(maNormalHandle, 3, GLES20.GL_FLOAT, false,
                        0, mTriangleNormal);
                GLES20.glEnableVertexAttribArray(maNormalHandle);
            }

            if ((mVertexShaderMode & SHADER_USE_TEXTURE) > 0) {
	        /* UV texture mapping */
                mTriangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET);
                GLES20.glVertexAttribPointer(maTextureHandle, 2, GLES20.GL_FLOAT, false,
                        TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices);
                GLES20.glEnableVertexAttribArray(maTextureHandle);
            }

            long time = SystemClock.uptimeMillis() % 10000L;
            float angle = (360.0f / 10000.0f) * ((int) time);

            Matrix.setIdentityM(mModelMatrix, 0);
            Matrix.translateM(mModelMatrix, 0, 0.0f, 0.0f, -5.0f);

            Matrix.setRotateM(mModelMatrix, 0, angle, 0, 0, 1.0f);
            Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);

            if ((mVertexShaderMode & SHADER_USE_MVMATRIX) > 0) {
                // Pass in the modelview matrix.
                GLES20.glUniformMatrix4fv(muMVMatrixHandle, 1, false, mMVPMatrix, 0);
            }

            // This multiplies the modelview matrix by the projection matrix, and stores the result in the MVP matrix
            // (which now contains model * view * projection).
            Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mMVPMatrix, 0);

            if ((mVertexShaderMode & SHADER_USE_MVPMATRIX) > 0) {
                GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mMVPMatrix, 0);
            }

            // Pass in the light position in eye space.
            if ((mVertexShaderMode & SHADER_USE_LIGHT) > 0) {
                // Calculate position of the light. Rotate and then push into the distance.
                Matrix.setIdentityM(mLightModelMatrix, 0);
                Matrix.translateM(mLightModelMatrix, 0, 0.0f, 0.0f, -5.0f);
                Matrix.rotateM(mLightModelMatrix, 0, angle, 0.0f, 0.0f, 1.0f);
                Matrix.translateM(mLightModelMatrix, 0, 0.0f, 0.0f, 2.0f);

                Matrix.multiplyMV(mLightPosInWorldSpace, 0, mLightModelMatrix, 0, mLightPosInModelSpace, 0);
                Matrix.multiplyMV(mLightPosInEyeSpace, 0, mViewMatrix, 0, mLightPosInWorldSpace, 0);
                GLES20.glUniform3f(muLightPosHandle, mLightPosInEyeSpace[0], mLightPosInEyeSpace[1], mLightPosInEyeSpace[2]);
            }

            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mNumVertices);

            checkGlError("glDrawArrays");
        }

        mFrameCount++;
    }

    /**
     * Draws a point representing the position of the light.
     */
    private void drawLight()
    {
        final int pointMVPMatrixHandle = GLES20.glGetUniformLocation(mPointProgramHandle, "uMVPMatrix");
        final int pointPositionHandle = GLES20.glGetAttribLocation(mPointProgramHandle, "aPosition");

        // Pass in the position.
        GLES20.glVertexAttrib3f(pointPositionHandle, mLightPosInModelSpace[0], mLightPosInModelSpace[1], mLightPosInModelSpace[2]);

        // Since we are not using a buffer object, disable vertex arrays for this attribute.
        GLES20.glDisableVertexAttribArray(pointPositionHandle);

        // Pass in the transformation matrix.
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mLightModelMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mMVPMatrix, 0);
        GLES20.glUniformMatrix4fv(pointMVPMatrixHandle, 1, false, mMVPMatrix, 0);

        // Draw the point.
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 1);
    }

    public void onSurfaceChanged(GL10 glUnused, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        float ratio = (float) width / height;
        Matrix.frustumM(mProjMatrix, 0, -ratio, ratio, -1, 1, 3, 7);
    }

    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
        // Set the background clear color to black.
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        // Use culling to remove back faces.
		//GLES20.glEnable(GLES20.GL_CULL_FACE);

        // Enable depth testing
		//GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        GLES20.glDisable(GLES20.GL_CULL_FACE);

        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
        GLES20.glDisable(GLES20.GL_POLYGON_OFFSET_FILL);
        GLES20.glDisable(GLES20.GL_SAMPLE_ALPHA_TO_COVERAGE);
        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glDisable(GLES20.GL_DITHER);
        GLES20.glDisable(GLES20.GL_STENCIL_TEST);

        if (mVertexShaderMode == VS_NONE) {
            mVertexShader = mVertexShader_none;
        } else if (mVertexShaderMode == VS_POSITION) {
            mVertexShader = mVertexShader_pos;
        } else if (mVertexShaderMode == VS_COLOR) {
            mVertexShader = mVertexShader_color;

        } else if (mVertexShaderMode == VS_LIGHT) {
            mVertexShader = mVertexShader_light;
        } else if (mVertexShaderMode == VS_LIGHT2) {
            mVertexShader = mVertexShader_per_vertex_light;
        } else if (mVertexShaderMode == VS_TEXTURE) {
            mVertexShader = mVertexShader_tex_light;
        } else if (mVertexShaderMode == VS_ORIGIN) {
            mVertexShader = mVertexShader_ori;
        } else {
            Log.e("MicroB", "Unknown vertex shader mode");
        }

        if (mFragmentShaderMode == FS_NONE) {
            mFragmentShader = mFragmentShader_none;
        } else if (mFragmentShaderMode == FS_COLOR) {
            mFragmentShader = mFragmentShader_color;
        } else if (mFragmentShaderMode == FS_LIGHT) {
            mFragmentShader = mFragmentShader_light;
        } else if (mFragmentShaderMode == FS_TEXTURE) {
            mFragmentShader = mFragmentShader_tex_light;
        } else if (mFragmentShaderMode == FS_ORIGIN) {
            mFragmentShader = mFragmentShader_ori;
        } else {
            Log.e("MicroB", "Unknown fragment shader mode");
        }


        final int vertexShaderHandle = ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER, mVertexShader);
        final int fragmentShaderHandle = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, mFragmentShader);

        mProgram = ShaderHelper.createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle, new String[] {
                POSITION_ATTRIBUTE, NORMAL_ATTRIBUTE, COLOR_ATTRIBUTE });

        if ((mVertexShaderMode & SHADER_USE_POSITION) > 0) {
            maPositionHandle = GLES20.glGetAttribLocation(mProgram, POSITION_ATTRIBUTE);
        }
        if ((mVertexShaderMode & SHADER_USE_COLOR) > 0) {
            maColorHandle = GLES20.glGetAttribLocation(mProgram, COLOR_ATTRIBUTE);
        }
        if ((mVertexShaderMode & SHADER_USE_NORMAL) > 0) {
            maNormalHandle = GLES20.glGetAttribLocation(mProgram, NORMAL_ATTRIBUTE);
        }
        if ((mVertexShaderMode & SHADER_USE_MVMATRIX) > 0) {
            muMVMatrixHandle = GLES20.glGetUniformLocation(mProgram, MV_MATRIX_UNIFORM);
        }
        if ((mVertexShaderMode & SHADER_USE_MVPMATRIX) > 0) {
            muMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, MVP_MATRIX_UNIFORM);
        }
        if ((mVertexShaderMode & SHADER_USE_LIGHT) > 0) {
            muLightPosHandle = GLES20.glGetUniformLocation(mProgram, LIGHTPOS_ATTRIBUTE);
        }
        if ((mVertexShaderMode & SHADER_USE_TEXTURE) > 0) {
            maTextureHandle = GLES20.glGetAttribLocation(mProgram, TEXTURE_ATTRIBUTE);
        }

        /*
         * Create our texture. This has to be done each time the
         * surface is created.
         */
        if (textureMode != 0) {
            int[] textures = new int[2];
            int target = GLES20.GL_TEXTURE_2D;
            GLES20.glGenTextures(2, textures, 0);

            mTextureID = textures[0];
            GLES20.glBindTexture(target, mTextureID);

            GLES20.glTexParameterf(target, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameterf(target, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

            GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
            GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);

            if (textureMode == 2) {
                InputStream is = mContext.getResources().openRawResource(R.drawable.tree64);
                try {
                    bitmap = BitmapFactory.decodeStream(is);
                } finally {
                    try {
                        is.close();
                    } catch(IOException e) {
                        // Ignore.
                    }
                }
                GLUtils.texImage2D(target, 0, bitmap, 0);
            } else if (textureMode == 3) {	// alternate two kinds of texture
                InputStream is0 = mContext.getResources().openRawResource(R.drawable.tree256);
                InputStream is1 = mContext.getResources().openRawResource(R.drawable.tree512);
                InputStream is2 = mContext.getResources().openRawResource(R.drawable.tree1024);
                InputStream is3 = mContext.getResources().openRawResource(R.drawable.tree2048);
                InputStream is4 = mContext.getResources().openRawResource(R.drawable.tree4096);
                try {
                    bitmap0 = BitmapFactory.decodeStream(is0);
                    bitmap1 = BitmapFactory.decodeStream(is1);
                    bitmap2 = BitmapFactory.decodeStream(is2);
                    bitmap3 = BitmapFactory.decodeStream(is3);
                    bitmap4 = BitmapFactory.decodeStream(is4);
                } finally {
                    try {
                        is0.close();
                        is1.close();
                        is2.close();
                        is3.close();
                        is4.close();
                    } catch(IOException e) {
                        // Ignore.
                    }
                }
                GLUtils.texImage2D(target, 0, bitmap0, 0);
                GLUtils.texImage2D(target, 0, bitmap1, 0);
                GLUtils.texImage2D(target, 0, bitmap2, 0);
                GLUtils.texImage2D(target, 0, bitmap3, 0);
                GLUtils.texImage2D(target, 0, bitmap4, 0);
            }else if (textureMode == 64) {
                InputStream is = mContext.getResources().openRawResource(R.drawable.tree64);
                try {
                    bitmap = BitmapFactory.decodeStream(is);
                } finally {
                    try {
                        is.close();
                    } catch(IOException e) {
                        // Ignore.
                    }
                }
                GLUtils.texImage2D(target, 0, bitmap, 0);
            }else if (textureMode == 128) {
                InputStream is = mContext.getResources().openRawResource(R.drawable.tree128);
                try {
                    bitmap = BitmapFactory.decodeStream(is);
                } finally {
                    try {
                        is.close();
                    } catch(IOException e) {
                        // Ignore.
                    }
                }
                GLUtils.texImage2D(target, 0, bitmap, 0);
            }else if (textureMode == 256) {
                InputStream is = mContext.getResources().openRawResource(R.drawable.tree256);
                try {
                    bitmap = BitmapFactory.decodeStream(is);
                } finally {
                    try {
                        is.close();
                    } catch(IOException e) {
                        // Ignore.
                    }
                }
                GLUtils.texImage2D(target, 0, bitmap, 0);
            }else if (textureMode == 512) {
                InputStream is = mContext.getResources().openRawResource(R.drawable.tree512);
                try {
                    bitmap = BitmapFactory.decodeStream(is);
                } finally {
                    try {
                        is.close();
                    } catch(IOException e) {
                        // Ignore.
                    }
                }
                GLUtils.texImage2D(target, 0, bitmap, 0);
            }else if (textureMode == 1024) {
                InputStream is = mContext.getResources().openRawResource(R.drawable.tree2048);
                try {
                    bitmap = BitmapFactory.decodeStream(is);
                } finally {
                    try {
                        is.close();
                    } catch(IOException e) {
                        // Ignore.
                    }
                }
                GLUtils.texImage2D(target, 0, bitmap, 0);
            }
        }

        Matrix.setLookAtM(mViewMatrix, 0, 0, 0, -5, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

        // Define a simple shader program for our point.
        final String pointVertexShader =
                "uniform mat4 uMVPMatrix;      \n"
                        +	"attribute vec4 aPosition;     \n"
                        + "void main()                    \n"
                        + "{                              \n"
                        + "   gl_Position = uMVPMatrix   \n"
                        + "               * aPosition;   \n"
                        + "   gl_PointSize = 5.0;         \n"
                        + "}                              \n";

        final String pointFragmentShader =
                "precision mediump float;       \n"
                        + "void main()                    \n"
                        + "{                              \n"
                        + "   gl_FragColor = vec4(1.0,    \n"
                        + "   1.0, 1.0, 1.0);             \n"
                        + "}                              \n";

        final int pointVertexShaderHandle = ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER, pointVertexShader);
        final int pointFragmentShaderHandle = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, pointFragmentShader);
        mPointProgramHandle = ShaderHelper.createAndLinkProgram(pointVertexShaderHandle, pointFragmentShaderHandle,
                new String[] {"aPosition"});

        lastSeenTime = System.currentTimeMillis();
    }

    private int loadShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        if (shader != 0) {
            GLES20.glShaderSource(shader, source);
            GLES20.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                Log.e(TAG, "Could not compile shader " + shaderType + ":");
                Log.e(TAG, GLES20.glGetShaderInfoLog(shader));
                GLES20.glDeleteShader(shader);
                shader = 0;
            }
        }
        return shader;
    }

    private int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        if (vertexShader == 0) {
            return 0;
        }

        int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        if (pixelShader == 0) {
            return 0;
        }

        int program = GLES20.glCreateProgram();
        if (program != 0) {
            GLES20.glAttachShader(program, vertexShader);
            checkGlError("glAttachShader");
            GLES20.glAttachShader(program, pixelShader);
            checkGlError("glAttachShader");
            GLES20.glLinkProgram(program);
            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] != GLES20.GL_TRUE) {
                Log.e(TAG, "Could not link program: ");
                Log.e(TAG, GLES20.glGetProgramInfoLog(program));
                GLES20.glDeleteProgram(program);
                program = 0;
            }
        }
        return program;
    }

    private void checkGlError(String op) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, op + ": glError " + error);
            throw new RuntimeException(op + ": glError " + error);
        }
    }

    private static final int FLOAT_SIZE_BYTES = 4;
    private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES;
    private static final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;
    private static final int TRIANGLE_VERTICES_DATA_UV_OFFSET = 3;

    private static final float[] mTriangleColorData;
    private static final float[] mTriangleVerticesData;
    private static final float[] mTriangleNormalData;
    static {
        int numTrianglePack = 1000;
        int stride;
        float[] baseVertexData = {
                // X, Y, Z, U, V -- 24
                0.0f, 0.0f, 0.0f, -0.5f, 0.0f,
                0.0f, 2.0f, 0.0f, 1.5f, -0.0f,
                2.0f, 2.0f, 0.0f, 0.5f,  1.61803399f,

                0.0f, 0.0f, 0.0f, -0.5f, 0.0f,
                2.0f, 0.0f, 0.0f, 1.5f, -0.0f,
                2.0f, 2.0f, 0.0f, 0.5f,  1.61803399f,

                0.0f, 0.0f, 0.0f, -0.5f, 0.0f,
                0.0f, 2.0f, 0.0f, 1.5f, -0.0f,
                -2.0f, 2.0f, 0.0f, 0.5f,  1.61803399f,

                0.0f, 0.0f, 0.0f, -0.5f, 0.0f,
                -2.0f, 0.0f, 0.0f, 1.5f, -0.0f,
                -2.0f, 2.0f, 0.0f, 0.5f,  1.61803399f,

                0.0f, 0.0f, 0.0f, -0.5f, 0.0f,
                -2.0f, 0.0f, 0.0f, 1.5f, -0.0f,
                -2.0f, -2.0f, 0.0f, 0.5f,  1.61803399f,

                0.0f, 0.0f, 0.0f, -0.5f, 0.0f,
                0.0f, -2.0f, 0.0f, 1.5f, -0.0f,
                -2.0f, -2.0f, 0.0f, 0.5f,  1.61803399f,

                0.0f, 0.0f, 0.0f, -0.5f, 0.0f,
                2.0f, 0.0f, 0.0f, 1.5f, -0.0f,
                2.0f, -2.0f, 0.0f, 0.5f,  1.61803399f,

                0.0f, 0.0f, 0.0f, -0.5f, 0.0f,
                0.0f, -2.0f, 0.0f, 1.5f, -0.0f,
                2.0f,  -2.0f, 0.0f, 0.5f,  1.61803399f,



                2.0f, 0.0f, 0.0f, -0.5f, 0.0f,
                2.0f, 2.0f, 0.0f, 1.5f, -0.0f,
                4.0f, 2.0f, 0.0f, 0.5f,  1.61803399f,

                2.0f, 0.0f, 0.0f, -0.5f, 0.0f,
                4.0f, 0.0f, 0.0f, 1.5f, -0.0f,
                4.0f, 2.0f, 0.0f, 0.5f,  1.61803399f,

                2.0f, 2.0f, 0.0f, -0.5f, 0.0f,
                2.0f, 4.0f, 0.0f, 1.5f, -0.0f,
                4.0f, 4.0f, 0.0f, 0.5f,  1.61803399f,

                2.0f, 2.0f, 0.0f, -0.5f, 0.0f,
                4.0f, 2.0f, 0.0f, 1.5f, -0.0f,
                4.0f, 4.0f, 0.0f, 0.5f,  1.61803399f,

                0.0f, 2.0f, 0.0f, -0.5f, 0.0f,
                0.0f, 4.0f, 0.0f, 1.5f, -0.0f,
                2.0f, 4.0f, 0.0f, 0.5f,  1.61803399f,

                0.0f, 2.0f, 0.0f, -0.5f, 0.0f,
                2.0f, 2.0f, 0.0f, 1.5f, -0.0f,
                2.0f, 4.0f, 0.0f, 0.5f,  1.61803399f,

                -2.0f, 2.0f, 0.0f, -0.5f, 0.0f,
                -2.0f, 4.0f, 0.0f, 1.5f, -0.0f,
                0.0f, 4.0f, 0.0f, 0.5f,  1.61803399f,

                -2.0f, 2.0f, 0.0f, -0.5f, 0.0f,
                0.0f, 2.0f, 0.0f, 1.5f, -0.0f,
                0.0f, 4.0f, 0.0f, 0.5f,  1.61803399f,



                -4.0f, 2.0f, 0.0f, -0.5f, 0.0f,
                -4.0f, 4.0f, 0.0f, 1.5f, -0.0f,
                -2.0f, 4.0f, 0.0f, 0.5f,  1.61803399f,

                -4.0f, 2.0f, 0.0f, -0.5f, 0.0f,
                -2.0f, 2.0f, 0.0f, 1.5f, -0.0f,
                -2.0f, 4.0f, 0.0f, 0.5f,  1.61803399f,

                -4.0f, 0.0f, 0.0f, -0.5f, 0.0f,
                -4.0f, 2.0f, 0.0f, 1.5f, -0.0f,
                -2.0f, 2.0f, 0.0f, 0.5f,  1.61803399f,

                -4.0f, 0.0f, 0.0f, -0.5f, 0.0f,
                -2.0f, 0.0f, 0.0f, 1.5f, -0.0f,
                -2.0f, 2.0f, 0.0f, 0.5f,  1.61803399f,

                2.0f, -2.0f, 0.0f, -0.5f, 0.0f,
                2.0f, 0.0f, 0.0f, 1.5f, -0.0f,
                4.0f, 0.0f, 0.0f, 0.5f,  1.61803399f,

                2.0f, -2.0f, 0.0f, -0.5f, 0.0f,
                4.0f, -2.0f, 0.0f, 1.5f, -0.0f,
                4.0f, 0.0f, 0.0f, 0.5f,  1.61803399f,

                2.0f, -4.0f, 0.0f, -0.5f, 0.0f,
                2.0f, -2.0f, 0.0f, 1.5f, -0.0f,
                4.0f, -2.0f, 0.0f, 0.5f,  1.61803399f,

                2.0f, -4.0f, 0.0f, -0.5f, 0.0f,
                4.0f, -4.0f, 0.0f, 1.5f, -0.0f,
                4.0f, -2.0f, 0.0f, 0.5f,  1.61803399f,



                0.0f, -4.0f, 0.0f, -0.5f, 0.0f,
                0.0f, -2.0f, 0.0f, 1.5f, -0.0f,
                2.0f, -2.0f, 0.0f, 0.5f,  1.61803399f,

                0.0f, -4.0f, 0.0f, -0.5f, 0.0f,
                2.0f, -4.0f, 0.0f, 1.5f, -0.0f,
                2.0f, -2.0f, 0.0f, 0.5f,  1.61803399f,

                -2.0f, -4.0f, 0.0f, -0.5f, 0.0f,
                -2.0f, -2.0f, 0.0f, 1.5f, -0.0f,
                0.0f, -2.0f, 0.0f, 0.5f,  1.61803399f,

                -2.0f, -4.0f, 0.0f, -0.5f, 0.0f,
                0.0f, -4.0f, 0.0f, 1.5f, -0.0f,
                0.0f, -2.0f, 0.0f, 0.5f,  1.61803399f,

                -4.0f, -4.0f, 0.0f, -0.5f, 0.0f,
                -4.0f, -2.0f, 0.0f, 1.5f, -0.0f,
                -2.0f, -2.0f, 0.0f, 0.5f,  1.61803399f,

                -4.0f, -4.0f, 0.0f, -0.5f, 0.0f,
                -2.0f, -4.0f, 0.0f, 1.5f, -0.0f,
                -2.0f, -2.0f, 0.0f, 0.5f,  1.61803399f,

                -4.0f, -2.0f, 0.0f, -0.5f, 0.0f,
                -4.0f, 0.0f, 0.0f, 1.5f, -0.0f,
                -2.0f, 0.0f, 0.0f, 0.5f,  1.61803399f,

                -4.0f, -2.0f, 0.0f, -0.5f, 0.0f,
                -2.0f, -2.0f, 0.0f, 1.5f, -0.0f,
                -2.0f, 0.0f, 0.0f, 0.5f,  1.61803399f,

				// In OpenGL counter-clockwise winding is default. This means that when we look at a triangle,
				// if the points are counter-clockwise we are looking at the "front". If not we are looking at
				// the back. OpenGL has an optimization where all back-facing triangles are culled, since they
				// usually represent the backside of an object and aren't visible anyways.
        };

        float[] baseColorData = {
                1.0f, 0.0f, 0.0f, 1.0f,
                0.0f, 1.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f, 1.0f,

                1.0f, 0.0f, 0.0f, 1.0f,
                0.0f, 1.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f, 1.0f,

                1.0f, 0.0f, 0.0f, 1.0f,
                0.0f, 1.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f, 1.0f,

                1.0f, 0.0f, 0.0f, 1.0f,
                0.0f, 1.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f, 1.0f,

                1.0f, 0.0f, 0.0f, 1.0f,
                0.0f, 1.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f, 1.0f,

                1.0f, 0.0f, 0.0f, 1.0f,
                0.0f, 1.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f, 1.0f,

                1.0f, 0.0f, 0.0f, 1.0f,
                0.0f, 1.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f, 1.0f,

                1.0f, 0.0f, 0.0f, 1.0f,
                0.0f, 1.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f, 1.0f,



                1.0f, 0.0f, 0.0f, 1.0f,
                0.0f, 1.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f, 1.0f,

                1.0f, 0.0f, 0.0f, 1.0f,
                0.0f, 1.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f, 1.0f,

                1.0f, 0.0f, 0.0f, 1.0f,
                0.0f, 1.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f, 1.0f,

                1.0f, 0.0f, 0.0f, 1.0f,
                0.0f, 1.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f, 1.0f,

                1.0f, 0.0f, 0.0f, 1.0f,
                0.0f, 1.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f, 1.0f,

                1.0f, 0.0f, 0.0f, 1.0f,
                0.0f, 1.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f, 1.0f,

                1.0f, 0.0f, 0.0f, 1.0f,
                0.0f, 1.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f, 1.0f,

                1.0f, 0.0f, 0.0f, 1.0f,
                0.0f, 1.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f, 1.0f,



                1.0f, 0.0f, 0.0f, 1.0f,
                0.0f, 1.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f, 1.0f,

                1.0f, 0.0f, 0.0f, 1.0f,
                0.0f, 1.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f, 1.0f,

                1.0f, 0.0f, 0.0f, 1.0f,
                0.0f, 1.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f, 1.0f,

                1.0f, 0.0f, 0.0f, 1.0f,
                0.0f, 1.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f, 1.0f,

                1.0f, 0.0f, 0.0f, 1.0f,
                0.0f, 1.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f, 1.0f,

                1.0f, 0.0f, 0.0f, 1.0f,
                0.0f, 1.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f, 1.0f,

                1.0f, 0.0f, 0.0f, 1.0f,
                0.0f, 1.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f, 1.0f,

                1.0f, 0.0f, 0.0f, 1.0f,
                0.0f, 1.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f, 1.0f,



                1.0f, 0.0f, 0.0f, 1.0f,
                0.0f, 1.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f, 1.0f,

                1.0f, 0.0f, 0.0f, 1.0f,
                0.0f, 1.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f, 1.0f,

                1.0f, 0.0f, 0.0f, 1.0f,
                0.0f, 1.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f, 1.0f,

                1.0f, 0.0f, 0.0f, 1.0f,
                0.0f, 1.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f, 1.0f,

                1.0f, 0.0f, 0.0f, 1.0f,
                0.0f, 1.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f, 1.0f,

                1.0f, 0.0f, 0.0f, 1.0f,
                0.0f, 1.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f, 1.0f,

                1.0f, 0.0f, 0.0f, 1.0f,
                0.0f, 1.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f, 1.0f,

                1.0f, 0.0f, 0.0f, 1.0f,
                0.0f, 1.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f, 1.0f,
        };
        float[] baseNormalData = {
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,

                0.0f, 0.0f, -1.0f,
                0.0f, 0.0f, -1.0f,
                0.0f, 0.0f, -1.0f,

                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,

                0.0f, 0.0f, -1.0f,
                0.0f, 0.0f, -1.0f,
                0.0f, 0.0f, -1.0f,

                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,

                0.0f, 0.0f, -1.0f,
                0.0f, 0.0f, -1.0f,
                0.0f, 0.0f, -1.0f,

                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,

                0.0f, 0.0f, -1.0f,
                0.0f, 0.0f, -1.0f,
                0.0f, 0.0f, -1.0f,



                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,

                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,

                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,

                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,

                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,

                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,

                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,

                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,



                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,

                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,

                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,

                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,

                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,

                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,

                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,

                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,



                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,

                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,

                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,

                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,

                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,

                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,

                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,

                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,
        };

        stride = 3 * 5 * 32;		// 3 vertex * 5 data * 12 triangles
        mTriangleVerticesData = new float[numTrianglePack * stride];
        for (int i = 0; i < numTrianglePack; ++i) {
            System.arraycopy(baseVertexData, 0, mTriangleVerticesData, i * stride, baseVertexData.length);
        }
        stride = 3 * 4 * 32;
        mTriangleColorData = new float[numTrianglePack * stride];
        for (int i = 0; i < numTrianglePack; ++i) {
            System.arraycopy(baseColorData, 0, mTriangleColorData, i * stride, baseColorData.length);
        }
        stride = 3 * 3 * 32;
        mTriangleNormalData = new float[numTrianglePack * stride];
        for (int i = 0; i < numTrianglePack; ++i) {
            System.arraycopy(baseNormalData, 0, mTriangleNormalData, i * stride, baseNormalData.length);
        }
    }

    private FloatBuffer mTriangleColor;
    private FloatBuffer mTriangleNormal;
    private FloatBuffer mTriangleVertices;

    private final String mVertexShader_tex_light =
            "uniform mat4 uMVPMatrix;\n" +
                    "uniform mat4 uMVMatrix;\n" +
                    "attribute vec4 aPosition;\n" +
                    "attribute vec4 aColor;\n" +
                    "attribute vec3 aNormal;\n" +
                    "attribute vec2 aTextureCoord;\n" +
                    "varying vec3 vPosition;\n" +
                    "varying vec4 vColor;\n" +
                    "varying vec3 vNormal;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "void main() {\n" +
                    "  vPosition = vec3(uMVMatrix * aPosition);\n" +
                    "  vColor = aColor;\n" +
                    "  vNormal = vec3(uMVMatrix * vec4(aNormal, 0.0));\n" +
                    "  vTextureCoord = aTextureCoord;\n" +
                    "  gl_Position = uMVPMatrix * aPosition;\n" +
                    "}\n";

    private final String mFragmentShader_tex_light =
            "precision mediump float;\n" +
                    "uniform vec3 uLightPos;\n" +
                    "uniform sampler2D sTexture;\n" +
                    "varying vec3 vPosition;\n" +
                    "varying vec3 vNormal;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "void main() {\n" +
                    "  float distance = length(uLightPos - vPosition);\n" +
                    "  vec3 lightVector = normalize(uLightPos - vPosition);\n" +
                    "  float diffuse;\n" +
                    "  if (gl_FrontFacing) {\n" +
                    "     diffuse = max(dot(vNormal, lightVector), 0.0);\n" +
                    "  } else {\n" +
                    "  	  diffuse = max(dot(-vNormal, lightVector), 0.0);\n" +
                    "  }\n" +
                    "  diffuse = diffuse * (1.0 / (1.0 + (0.10 * distance)));\n" +
                    "  diffuse = diffuse + 0.99;\n" +
                    "  gl_FragColor = (diffuse * texture2D(sTexture, vTextureCoord));\n" +
                    "}\n";

    private final String mVertexShader_per_vertex_light =
            "uniform mat4 uMVPMatrix;      \n"     // A constant representing the combined model/view/projection matrix.
                    + "uniform mat4 uMVMatrix;       \n"     // A constant representing the combined model/view matrix.
                    + "uniform vec3 uLightPos;       \n"     // The position of the light in eye space.
                    + "attribute vec4 aPosition;     \n"     // Per-vertex position information we will pass in.
                    + "attribute vec4 aColor;        \n"     // Per-vertex color information we will pass in.
                    + "attribute vec3 aNormal;       \n"     // Per-vertex normal information we will pass in.
                    + "varying vec4 vColor;          \n"     // This will be passed into the fragment shader.
                    + "void main()                    \n"     // The entry point for our vertex shader.
                    + "{                              \n"
                    + "   int i;\n"
                    + "   for (i = 0; i < 1; ++i) {\n"
                    + "   vec3 modelViewVertex = vec3(uMVMatrix * aPosition);              \n"
                    + "   vec3 modelViewNormal = vec3(uMVMatrix * vec4(aNormal, 0.0));     \n"
                    + "   float distance = length(uLightPos - modelViewVertex);             \n"
                    + "   vec3 lightVector = normalize(uLightPos - modelViewVertex);        \n"
                    + "   float diffuse = max(dot(modelViewNormal, lightVector), 0.1);       \n"
                    + "   diffuse = diffuse * (1.0 / (1.0 + (0.25 * distance * distance)));  \n"
                    + "   vColor = aColor * (diffuse + 0.99);                                       \n"
                    + "   gl_Position = uMVPMatrix * aPosition;                            \n"
                    + "   }\n"
                    + "}                                                                     \n";

    private final String mVertexShader_light =
            "uniform mat4 uMVPMatrix;\n" +
                    "uniform mat4 uMVMatrix;\n" +
                    "attribute vec4 aPosition;\n" +
                    "attribute vec4 aColor;\n" +
                    "attribute vec3 aNormal;\n" +
                    "varying vec4 vColor;\n" +
                    "varying vec3 vNormal;\n" +
                    "varying vec3 vPosition;\n" +
                    "void main() {\n" +
                    "  vPosition = vec3(uMVMatrix * aPosition);\n" +
                    "  vColor = aColor;\n" +
                    "  vNormal = vec3(uMVMatrix * vec4(aNormal, 0.0));\n" +
                    "  gl_Position = uMVPMatrix * aPosition;\n" +
                    "}\n";

    private final String mFragmentShader_light =
            "precision mediump float;\n" +
                    "uniform vec3 uLightPos;\n" +
                    "varying vec3 vPosition;\n" +
                    "varying vec4 vColor;\n" +
                    "varying vec3 vNormal;\n" +
                    "void main() {\n" +
                    "  float distance = length(uLightPos - vPosition);\n" +
                    "  vec3 lightVector = normalize(uLightPos - vPosition);\n" +
                    "  float diffuse;\n" +
                    "  if (gl_FrontFacing) {\n" +
                    "     diffuse = max(dot(vNormal, lightVector), 0.0);\n" +
                    "  } else {\n" +
                    "  	  diffuse = max(dot(-vNormal, lightVector), 0.0);\n" +
                    "  }\n" +
                    "  diffuse = diffuse * (1.0 / (1.0 + (0.10 * distance)));\n" +
                    "  diffuse = diffuse + 0.99;\n" +
                    "  gl_FragColor = (vColor * diffuse);\n" +
                    "}\n";

    private final String mVertexShader_color =
            "uniform mat4 uMVPMatrix;\n" +
                    "attribute vec4 aPosition;\n" +
                    "attribute vec4 aColor;\n" +
                    "varying vec4 vColor;\n" +
                    "void main() {\n" +
                    "  vColor = aColor;\n" +
                    "  gl_Position = uMVPMatrix * aPosition;\n" +
                    "}\n";

    private final String mFragmentShader_color =
            "precision mediump float;\n" +
                    "varying vec4 vColor;\n" +
                    "void main() {\n" +
                    "  gl_FragColor = vColor;\n" +
                    "}\n";

    private final String mVertexShader_pos =
            "uniform mat4 uMVPMatrix;\n" +
                    "attribute vec4 aPosition;\n" +
                    "void main() {\n" +
                    "  gl_Position = uMVPMatrix * aPosition;\n" +
                    "}\n";

    private final String mVertexShader_none =
            "void main() {\n" +
                    "}\n";

    private final String mFragmentShader_none =
            "precision mediump float;\n" +
                    "void main() {\n" +
                    "}\n";

    // original
    private final String mVertexShader_ori =
            "uniform mat4 uMVPMatrix;\n" +
                    "attribute vec4 aPosition;\n" +
                    "attribute vec2 aTextureCoord;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "void main() {\n" +
                    "  vTextureCoord = aTextureCoord;\n" +
                    "  gl_Position = uMVPMatrix * aPosition;\n" +
                    "}\n";


    private final String mFragmentShader_ori =
            "precision mediump float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform sampler2D sTexture;\n" +
                    "void main() {\n" +
                    "  gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
                    "}\n";

    private String mVertexShader;
    private String mFragmentShader;


    private float[] mMVPMatrix = new float[16];
    private float[] mProjMatrix = new float[16];
    private float[] mModelMatrix = new float[16];
    private float[] mViewMatrix = new float[16];

    private int mProgram;
    private int mPointProgramHandle;
    private int mTextureID;
    private int muMVPMatrixHandle;
    private int muMVMatrixHandle;
    private int maPositionHandle;
    private int maColorHandle;
    private int maNormalHandle;
    private int muLightPosHandle;
    private int maTextureHandle;

    private Context mContext;
    private final Activity mActivity;
    private static String TAG = "GLES20TriangleRenderer";
}