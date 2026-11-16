import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

/** Real GPU regression for drawing depth-tested world effects after Fabulous compositing.
 * This reproduces the framebuffer failure, not a full Minecraft client or multiplayer test.
 */
public final class InnerDragonsDepthTest {
    private static final int WIDTH = 64, HEIGHT = 32;
    private static int checks;

    public static void main(String[] args) {
        GLFWErrorCallback errors = GLFWErrorCallback.createPrint(System.err);
        errors.set();
        if (!GLFW.glfwInit()) throw new AssertionError("GLFW initialization failed");
        long window = 0;
        try {
            GLFW.glfwDefaultWindowHints();
            GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
            window = GLFW.glfwCreateWindow(WIDTH, HEIGHT, "Hidden cutscene depth regression", 0, 0);
            if (window == 0) throw new AssertionError("Hidden OpenGL context creation failed");
            GLFW.glfwMakeContextCurrent(window);
            GL.createCapabilities();
            check(GL.getCapabilities().OpenGL30, "OpenGL framebuffer blit support is required");
            System.out.println("GPU: " + glGetString(GL_RENDERER));
            glViewport(0, 0, WIDTH, HEIGHT);
            glDisable(GL_TEXTURE_2D);
            glDisable(GL_BLEND);
            glDisable(GL_CULL_FACE);
            glMatrixMode(GL_PROJECTION);
            glLoadIdentity();
            glMatrixMode(GL_MODELVIEW);
            glLoadIdentity();
            Frame scene = new Frame(), main = new Frame();
            try {
                for (float effectDepth : new float[]{.90F, .95F, .99F}) {
                    verifyDepthRecovery(scene, main, effectDepth);
                }
                check(glGetError() == GL_NO_ERROR, "The depth recovery path generated an OpenGL error");
            } finally {
                scene.close();
                main.close();
            }
            System.out.println("Inner Dragons Fabulous depth: " + checks + " GPU checks passed.");
        } finally {
            if (window != 0) GLFW.glfwDestroyWindow(window);
            GLFW.glfwTerminate();
            errors.free();
        }
    }

    private static void verifyDepthRecovery(Frame scene, Frame main, float effectDepth) {
        // The particles target preserves the original scene depth before Fabulous's final blit.
        scene.bind();
        glDepthMask(true);
        glClearDepth(1);
        glClearColor(.2F, .2F, .2F, 1);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
        rectangle(-1, 0, .8F, .2F, .2F, .2F);
        near(depth(WIDTH / 4), .8F, "terrain depth must be retained on the left");
        near(depth(WIDTH * 3 / 4), 1, "empty scene depth must be retained on the right");

        // Vanilla blit.vsh sets clip-space z=.2, w=1. Shader.process clears the
        // destination and uses ALWAYS, leaving depth .6 across the composited output.
        main.bind();
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glDepthFunc(GL_ALWAYS);
        rectangle(-1, 1, .6F, .2F, .2F, .2F);
        near(depth(WIDTH / 4), .6F, "compositor must reproduce the overwritten scene depth");
        near(depth(WIDTH * 3 / 4), .6F, "compositor must overwrite empty-space depth too");
        glDepthFunc(GL_LEQUAL);
        glDepthMask(false); // Same COLOR_WRITE depth policy as the cutscene geometry.
        rectangle(-1, 1, effectDepth, 0, 1, 0);
        check(!green(WIDTH / 4) && !green(WIDTH * 3 / 4),
                "the old path should hide every world-effect fragment after the compositor");

        // Equivalent operation to main.copyDepthFrom(particlesTarget), then bindWrite(false).
        glBindFramebuffer(GL_READ_FRAMEBUFFER, scene.framebuffer);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, main.framebuffer);
        glBlitFramebuffer(0, 0, WIDTH, HEIGHT, 0, 0, WIDTH, HEIGHT, GL_DEPTH_BUFFER_BIT, GL_NEAREST);
        main.bind();
        near(depth(WIDTH / 4), .8F, "restoration must recover actual terrain depth");
        near(depth(WIDTH * 3 / 4), 1, "restoration must recover actual empty-space depth");
        rectangle(-1, 1, effectDepth, 0, 1, 0);
        check(!green(WIDTH / 4), "the restored effects must still be hidden behind terrain");
        check(green(WIDTH * 3 / 4), "the restored effects must be visible in front of empty space");
        near(depth(WIDTH * 3 / 4), 1, "transparent effects must not overwrite restored scene depth");
    }

    private static void rectangle(float left, float right, float depth, float r, float g, float b) {
        glColor4f(r, g, b, 1);
        float z = depth * 2 - 1;
        glBegin(GL_QUADS);
        glVertex3f(left, -1, z);
        glVertex3f(right, -1, z);
        glVertex3f(right, 1, z);
        glVertex3f(left, 1, z);
        glEnd();
    }

    private static float depth(int x) {
        FloatBuffer value = BufferUtils.createFloatBuffer(1);
        glReadPixels(x, HEIGHT / 2, 1, 1, GL_DEPTH_COMPONENT, GL_FLOAT, value);
        return value.get(0);
    }

    private static boolean green(int x) {
        ByteBuffer value = BufferUtils.createByteBuffer(4);
        glReadPixels(x, HEIGHT / 2, 1, 1, GL_RGBA, GL_UNSIGNED_BYTE, value);
        return (value.get(0) & 255) < 10 && (value.get(1) & 255) > 245 && (value.get(2) & 255) < 10;
    }

    private static void near(float actual, float expected, String message) {
        check(Math.abs(actual - expected) < .0001F, message + ": " + actual);
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        checks++;
    }

    private static final class Frame {
        final int framebuffer = glGenFramebuffers();
        final int color = glGenRenderbuffers();
        final int depth = glGenRenderbuffers();
        Frame() {
            bind();
            glBindRenderbuffer(GL_RENDERBUFFER, color);
            glRenderbufferStorage(GL_RENDERBUFFER, GL_RGBA8, WIDTH, HEIGHT);
            glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER, color);
            glBindRenderbuffer(GL_RENDERBUFFER, depth);
            glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, WIDTH, HEIGHT);
            glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depth);
            check(glCheckFramebufferStatus(GL_FRAMEBUFFER) == GL_FRAMEBUFFER_COMPLETE,
                    "Regression framebuffer must be complete");
        }
        void bind() { glBindFramebuffer(GL_FRAMEBUFFER, framebuffer); }
        void close() {
            glDeleteRenderbuffers(color);
            glDeleteRenderbuffers(depth);
            glDeleteFramebuffers(framebuffer);
        }
    }
}
