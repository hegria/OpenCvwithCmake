package com.example.opencvwithcmake;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import com.google.ar.core.Coordinates2d;
import com.google.ar.core.Frame;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class Background {
  int texID;
  int program;int buffer;
  public int tex2id;
  int[] textures = new int[2];
  FloatBuffer vb;
  FloatBuffer tb;
  private final String vscode = "" +
          "attribute vec3 vPosition;\n" +
          "attribute vec2 vTexcoord;\n" +
          "varying vec2 tc;\n" +

          "void main() {\n" +
          "  gl_Position = vec4(vPosition, 1.0);\n" +
          "  tc = vTexcoord;" +
          "}\n";

  private final String fscode = "" +
          "#extension GL_OES_EGL_image_external : require\n" +
          "" +
          "precision mediump float;" +
          "uniform sampler2D tex;" +
          "varying vec2 tc;\n" +

          "void main() {\n" +
          "  gl_FragColor = texture2D(tex, tc);\n" +
          "}\n";

  public Background() {
    float[] vertex = {
            1.0f, 1.0f, 0.0f, /*x, y, z*/ 0.0f, 0.0f,
            -1.0f, 1.0f, 0.0f, 0.0f, 1.0f,
            -1.0f, -1.0f, 0.0f, 1.0f, 1.0f,

            -1.0f, -1.0f, 0.0f, 1.0f, 1.0f,
            1.0f, -1.0f, 0.0f, 1.0f, 0.0f,
            1.0f, 1.0f, 0.0f, 0.0f, 0.0f
    };
    ByteBuffer bb = ByteBuffer.allocateDirect(vertex.length * 4);
    bb.order(ByteOrder.nativeOrder());
    FloatBuffer vb = bb.asFloatBuffer();
    vb.put(vertex);
    vb.position(0);

    int[] buffers = new int[1];
    GLES20.glGenBuffers(1, buffers, 0);
    buffer = buffers[0];

    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffer);
    GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertex.length * 4, vb, GLES20.GL_DYNAMIC_DRAW);
    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

    int vs = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
    GLES20.glShaderSource(vs, vscode);
    GLES20.glCompileShader(vs);
    int fs = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
    GLES20.glShaderSource(fs, fscode);
    GLES20.glCompileShader(fs);

    program = GLES20.glCreateProgram();
    GLES20.glAttachShader(program, vs);
    GLES20.glAttachShader(program, fs);
    GLES20.glLinkProgram(program);

    GLES20.glGenTextures(2, textures, 0);
    texID = textures[0];
    tex2id = textures[1];
    GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, tex2id);
    GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
    GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
    GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
    GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
    GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);

    /*GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texID);
    GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
    GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
    GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
    GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
    GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);

     */
  }

  public void draw(){


    GLES20.glUseProgram(program);

    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffer);
    int vpos = GLES20.glGetAttribLocation(program, "vPosition");
    GLES20.glEnableVertexAttribArray(vpos);
    GLES20.glVertexAttribPointer(vpos, 3, GLES20.GL_FLOAT, false, 5 * 4, 0);

    int tpos = GLES20.glGetAttribLocation(program, "vTexcoord");
    GLES20.glEnableVertexAttribArray(tpos);
    GLES20.glVertexAttribPointer(tpos, 2, GLES20.GL_FLOAT, false, 5 * 4, 3 * 4);
    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);



    GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texID);
    int pos = GLES20.glGetUniformLocation(program, "tex");
    GLES20.glUniform1i(pos, GLES20.GL_TEXTURE0);

    GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);

    GLES20.glDisableVertexAttribArray(vpos);
    GLES20.glDisableVertexAttribArray(tpos);
  }

}
