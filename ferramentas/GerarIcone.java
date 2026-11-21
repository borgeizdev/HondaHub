import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Gera o icone do app a partir de ferramentas/logo-app.png (simbolo com fundo transparente).
 *
 * Uso (na pasta do projeto):
 *   & "$env:LOCALAPPDATA\Android\jdk-17\bin\java.exe" ferramentas\GerarIcone.java
 *
 * Saida em app/src/main/res:
 *  - mipmap-<densidade>/ic_launcher.png          quadrado branco arredondado + simbolo (Android antigo)
 *  - mipmap-<densidade>/ic_launcher_frente.png   so o simbolo, na area segura (icone adaptavel, Android 8+)
 *  - mipmap-anydpi-v26/ic_launcher.xml           fundo branco + simbolo (o Android recorta no formato dele)
 */
public class GerarIcone {

    static final String[] DENS = {"mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi"};
    static final double[] FATOR = {1, 1.5, 2, 3, 4};
    /** Giro do simbolo em graus, sentido horario (90 = a ponta, que no arquivo aponta para a direita, aponta para baixo). */
    static final int GIRO = 90;

    public static void main(String[] a) throws Exception {
        BufferedImage logo = girar(recortar(ImageIO.read(new File("ferramentas/logo-app.png"))), GIRO);
        File res = new File("app/src/main/res");
        for (int i = 0; i < DENS.length; i++) {
            File pasta = new File(res, "mipmap-" + DENS[i]);
            pasta.mkdirs();
            int t = (int) Math.round(48 * FATOR[i]);
            ImageIO.write(classico(logo, t), "png", new File(pasta, "ic_launcher.png"));
            int tf = (int) Math.round(108 * FATOR[i]);
            ImageIO.write(frente(logo, tf), "png", new File(pasta, "ic_launcher_frente.png"));
            System.out.println("mipmap-" + DENS[i] + ": " + t + " px / adaptavel " + tf + " px");
        }
    }

    /** Quadrado branco de cantos arredondados com o simbolo no meio (64% do lado). */
    static BufferedImage classico(BufferedImage logo, int t) {
        BufferedImage o = new BufferedImage(t, t, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = qualidade(o.createGraphics());
        double m = t * 0.04; // folga em volta, como os icones do sistema
        double lado = t - 2 * m;
        g.setColor(Color.WHITE);
        g.fill(new RoundRectangle2D.Double(m, m, lado, lado, lado * 0.42, lado * 0.42));
        desenharCentro(g, logo, t, lado * 0.64);
        g.dispose();
        return o;
    }

    /** Camada da frente do icone adaptavel: 108 dp, simbolo dentro da area segura (46 dp). */
    static BufferedImage frente(BufferedImage logo, int t) {
        BufferedImage o = new BufferedImage(t, t, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = qualidade(o.createGraphics());
        desenharCentro(g, logo, t, t * 46.0 / 108.0);
        g.dispose();
        return o;
    }

    static void desenharCentro(Graphics2D g, BufferedImage logo, int t, double max) {
        double esc = max / Math.max(logo.getWidth(), logo.getHeight());
        double w = logo.getWidth() * esc, h = logo.getHeight() * esc;
        // Reduz em etapas (metade por vez) para o simbolo nao ficar serrilhado nos tamanhos pequenos
        BufferedImage img = logo;
        while (img.getWidth() / 2 > w) {
            BufferedImage meio = new BufferedImage(img.getWidth() / 2, img.getHeight() / 2, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gm = qualidade(meio.createGraphics());
            gm.drawImage(img, 0, 0, meio.getWidth(), meio.getHeight(), null);
            gm.dispose();
            img = meio;
        }
        g.drawImage(img, (int) Math.round((t - w) / 2), (int) Math.round((t - h) / 2), (int) Math.round(w), (int) Math.round(h), null);
    }

    /** Gira [im] em multiplos de 90 graus (sentido horario), sem perder nitidez. */
    static BufferedImage girar(BufferedImage im, int graus) {
        int voltas = ((graus / 90) % 4 + 4) % 4;
        for (int v = 0; v < voltas; v++) {
            int w = im.getWidth(), h = im.getHeight();
            BufferedImage o = new BufferedImage(h, w, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < h; y++)
                for (int x = 0; x < w; x++) o.setRGB(h - 1 - y, x, im.getRGB(x, y));
            im = o;
        }
        return im;
    }

    /** Corta as sobras transparentes em volta do simbolo. */
    static BufferedImage recortar(BufferedImage im) {
        int x0 = im.getWidth(), y0 = im.getHeight(), x1 = 0, y1 = 0;
        for (int y = 0; y < im.getHeight(); y++)
            for (int x = 0; x < im.getWidth(); x++)
                if ((im.getRGB(x, y) >>> 24) > 8) { x0 = Math.min(x0, x); x1 = Math.max(x1, x); y0 = Math.min(y0, y); y1 = Math.max(y1, y); }
        BufferedImage o = new BufferedImage(x1 - x0 + 1, y1 - y0 + 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = o.createGraphics();
        g.drawImage(im, -x0, -y0, null);
        g.dispose();
        return o;
    }

    static Graphics2D qualidade(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        return g;
    }
}
