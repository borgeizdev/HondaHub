import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Iterator;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

/**
 * Gera os papeis de parede "garagem" do launcher, um por cor.
 *
 * Uso (na pasta do projeto):
 *   & "$env:LOCALAPPDATA\Android\jdk-17\bin\java.exe" ferramentas\GerarFundos.java
 *
 * Saida: app/src/main/res/drawable-nodpi/fundo_<cor>.jpg
 * Para criar uma cor nova, acrescente uma linha em TEMAS e a mesma cor em Temas.kt.
 *
 * A garagem e uma sala 3D de verdade (piso, parede do fundo, laterais e teto)
 * vista por uma camera na MESMA altura/horizonte da foto do carro (carro.png
 * na posicao do launcher): assim o piso fica na perspectiva certa embaixo dele.
 * Medidas em metros; y = 0 e o chao, a camera olha para +z.
 */
public class GerarFundos {

    static final int W = 1600, H = 802;

    /** nome, cor da luz (r,g,b), cor da parede (r,g,b), forca da luz */
    static final Object[][] TEMAS = {
        {"ambar",    new double[]{255, 128, 40},  new double[]{46, 24, 12}, 1.00},
        {"vermelho", new double[]{255, 45, 35},   new double[]{44, 9, 9},   1.00},
        {"laranja",  new double[]{255, 95, 20},   new double[]{44, 16, 6},  1.00},
        {"amarelo",  new double[]{255, 200, 50},  new double[]{40, 32, 10}, 0.95},
        {"verde",    new double[]{50, 230, 110},  new double[]{8, 34, 18},  0.95},
        {"ciano",    new double[]{40, 210, 235},  new double[]{6, 30, 36},  0.95},
        {"azul",     new double[]{60, 130, 255},  new double[]{8, 18, 48},  1.05},
        {"roxo",     new double[]{150, 80, 255},  new double[]{22, 12, 46}, 1.05},
        {"rosa",     new double[]{255, 70, 170},  new double[]{44, 10, 30}, 1.00},
        {"vinho",    new double[]{200, 30, 70},   new double[]{32, 6, 14},  1.00},
        {"branco",   new double[]{255, 255, 255}, new double[]{54, 56, 62}, 0.85},
        {"cinza",    new double[]{190, 200, 215}, new double[]{34, 36, 40}, 0.90},
        {"grafite",  new double[]{150, 175, 210}, new double[]{18, 21, 26}, 0.80},
        {"preto",    new double[]{235, 235, 240}, new double[]{8, 8, 9},    0.70},
    };

    // ---- Camera (casada com a foto do carro) e sala
    static final double V0 = 0.47;     // horizonte (altura do olho) na tela, 0..1
    static final double F = 1.6;       // distancia focal (em alturas de tela)
    static final double CAM = 0.8125;  // altura da camera (m)
    static final double ZW = 10;       // distancia ate a parede do fundo
    static final double HC = 3.0;      // altura do teto
    static final double XW = 5.0;      // meia largura da sala
    static final double PISO = 1.0;    // tamanho da placa do piso

    // Tipos de superficie
    static final int NADA = 0, CHAO = 1, FUNDO = 2, LADO = 3, TETO = 4;

    static double[] L, P;
    static double FORCA;

    public static void main(String[] a) throws Exception {
        File pasta = new File(a.length > 0 ? a[0] : "app/src/main/res/drawable-nodpi");
        pasta.mkdirs();
        for (Object[] t : TEMAS) {
            L = div((double[]) t[1], 255);
            P = div((double[]) t[2], 255);
            FORCA = (Double) t[3];
            BufferedImage img = gerar();
            File f = new File(pasta, "fundo_" + t[0] + ".jpg");
            salvarJpg(img, f, 0.90f);
            System.out.println(f + "  " + f.length() / 1024 + " KB");
        }
    }

    static BufferedImage gerar() {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        java.util.Random rnd = new java.util.Random(7);
        double asp = (double) W / H;
        double vJunta = V0 + CAM * F / ZW; // linha parede/chao na tela

        for (int y = 0; y < H; y++) {
            double v = (y + 0.5) / H;
            for (int x = 0; x < W; x++) {
                double u = (x + 0.5) / W;
                double dx = (u - 0.5) * asp, dy = V0 - v, dz = F;
                double[] c = raio(0, CAM, 0, dx, dy, dz, rnd, true);

                // Brilho (bloom) da faixa de luz na base da parede
                double dv = (v - vJunta) * H;
                double du = Math.abs(u - 0.5);
                double hor = Math.exp(-du / 0.16) * 0.85 + Math.exp(-du / 0.40) * 0.30;
                double halo = Math.exp(-sq(dv / 8)) * hor * 0.40 * FORCA
                            + Math.exp(-sq(dv / 38)) * Math.exp(-sq(du / 0.28)) * 0.20 * FORCA;
                for (int k = 0; k < 3; k++) c[k] += halo * L[k];

                // Vinheta
                double ex = (u - 0.5) / 0.62, ey = (v - 0.50) / 0.62;
                double vin = 1 - 0.60 * suave(0.45, 1.15, Math.sqrt(ex * ex + ey * ey));
                for (int k = 0; k < 3; k++) c[k] *= vin;

                img.setRGB(x, y, cor(c, rnd));
            }
        }
        return img;
    }

    /** Acerta o raio na sala e devolve a cor vista. [primario] = raio da camera. */
    static double[] raio(double ox, double oy, double oz, double dx, double dy, double dz,
                         java.util.Random rnd, boolean primario) {
        double t = 1e18; int tipo = NADA;
        if (dy < 0 && primario) { double tt = -oy / dy; if (tt < t) { t = tt; tipo = CHAO; } }
        if (dy > 0) { double tt = (HC - oy) / dy; if (tt < t) { t = tt; tipo = TETO; } }
        if (dz > 0) { double tt = (ZW - oz) / dz; if (tt < t) { t = tt; tipo = FUNDO; } }
        if (dx != 0) { double tt = ((dx > 0 ? XW : -XW) - ox) / dx; if (tt > 0 && tt < t) { t = tt; tipo = LADO; } }
        double px = ox + dx * t, py = oy + dy * t, pz = oz + dz * t;
        switch (tipo) {
            case CHAO: return chao(px, pz, dx, dy, dz, rnd);
            case FUNDO: return paredeFundo(px, py);
            case LADO: return paredeLado(py, pz);
            case TETO: return teto(px, pz);
            default: return new double[3];
        }
    }

    /** Parede do fundo: lisa (textura leve de concreto), arandelas e faixa de luz na base. */
    static double[] paredeFundo(double x, double y) {
        double textura = 1 + (ruido(x * 2.2, y * 2.2) - 0.5) * 0.10 + (ruido(x * 9, y * 9) - 0.5) * 0.04;
        double vv = clampD((HC - y) / HC, 0, 1); // 0 no teto, 1 no chao

        double cones = 0;
        double[] xs = {-3.6, -1.8, 0, 1.8, 3.6};
        for (double cx : xs) {
            double larg = 0.10 + 0.95 * vv;
            double forma = Math.exp(-sq((x - cx) / larg));
            double queda = Math.exp(-vv * 1.1) * suave(0.0, 0.05, vv) * 1.5;
            double peso = 0.55 + 0.45 * Math.exp(-sq(cx / 3.5));
            cones += forma * queda * peso;
            cones += Math.exp(-sq((x - cx) / 0.06) - sq((vv - 0.015) / 0.02)) * 0.8;
        }
        double centro = Math.exp(-sq(x / 3.0)) * (0.25 + 0.75 * Math.pow(vv, 1.6));
        double ambiente = 0.42 + 0.20 * vv;

        double[] c = new double[3];
        for (int k = 0; k < 3; k++) {
            double difuso = P[k] * ambiente;
            double luz = (P[k] * 0.9 + L[k] * 0.22) * (cones * 0.9 + centro * 0.75) * FORCA;
            c[k] = (difuso + luz) * textura;
        }
        // Faixa de LED na base da parede (mais forte no meio)
        double ax = Math.abs(x);
        double perfil = Math.exp(-ax / 1.6) * 0.85 + Math.exp(-ax / 4.0) * 0.30;
        double faixa = cobreY(y, 0.012, 0.012) * perfil * 1.6 * FORCA;
        // Linha de LED onde a parede encontra o teto
        double topo = cobreY(HC - y, 0.010, 0.010) * (0.35 + 0.65 * Math.exp(-sq(x / 4.5))) * 0.55 * FORCA;
        for (int k = 0; k < 3; k++) c[k] += faixa * (L[k] * 0.55 + 0.6) + topo * (L[k] * 0.6 + 0.35);
        return c;
    }

    /** Paredes laterais: mais escuras, clareando perto do fundo, com LED rente ao chao. */
    static double[] paredeLado(double y, double z) {
        double textura = 1 + (ruido(z * 2.2, y * 2.2) - 0.5) * 0.10;
        double prof = clampD(z / ZW, 0, 1);
        double luz = 0.22 + 0.45 * Math.pow(prof, 2) * Math.exp(-sq((y - HC * 0.55) / 1.6));
        double[] c = new double[3];
        for (int k = 0; k < 3; k++) c[k] = (P[k] * luz + L[k] * 0.05 * prof * FORCA) * textura;
        double led = cobreY(y, 0.012, 0.02) * (0.15 + 0.55 * prof) * 0.6 * FORCA;
        for (int k = 0; k < 3; k++) c[k] += led * (L[k] * 0.7 + 0.25);
        return c;
    }

    /** Teto escuro com duas calhas de LED correndo para o fundo. */
    static double[] teto(double x, double z) {
        double prof = clampD(z / ZW, 0, 1);
        double[] c = new double[3];
        for (int k = 0; k < 3; k++) c[k] = P[k] * (0.16 + 0.14 * prof);
        double brilho = 0;
        for (double xs : new double[]{-1.9, 1.9}) {
            double dentro = suave(1.0, 2.0, z) * (1 - suave(ZW - 0.6, ZW - 0.3, z));
            brilho += cobreY(Math.abs(x - xs), 0.04, 0.02) * dentro * 0.42;
            brilho += Math.exp(-sq((x - xs) / 0.45)) * dentro * 0.05;
        }
        for (int k = 0; k < 3; k++) c[k] += brilho * FORCA * (L[k] * 0.55 + 0.45);
        return c;
    }

    /** Piso de porcelanato polido: placas com rejunte, luz embaixo do carro e reflexo real. */
    static double[] chao(double x, double z, double dx, double dy, double dz, java.util.Random rnd) {
        // Rejunte com antisserrilhado (tamanho de 1 px no chao)
        double pxX = z / (F * H) / PISO;
        double pxZ = z * z / (CAM * F * H) / PISO;
        double fx = frac(x / PISO + 0.5), fz = frac(z / PISO);
        double lx = cobre(Math.min(fx, 1 - fx), 0.004, pxX);
        double lz = cobre(Math.min(fz, 1 - fz), 0.004, pxZ);
        double rejunte = Math.max(lx * (1 - suave(0.05, 0.4, pxX)), lz * (1 - suave(0.05, 0.4, pxZ)));
        int tx = (int) Math.floor(x / PISO + 0.5), tz = (int) Math.floor(z / PISO);
        double peca = 1 + (hash(tx * 73856093 ^ tz * 19349663) - 0.5) * 0.08;

        // Reflexo: espelha o raio e borra um pouco (piso nao e espelho perfeito)
        double n = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double rx = dx / n, ry = -dy / n, rz = dz / n;
        double[] refl = new double[3];
        int amostras = 12;
        double aspereza = 0.035;
        for (int s = 0; s < amostras; s++) {
            double jx = rx + rnd.nextGaussian() * aspereza;
            double jy = Math.abs(ry + rnd.nextGaussian() * aspereza * 0.6);
            double[] r = raio(x, 0, z, jx, jy, rz, rnd, false);
            for (int k = 0; k < 3; k++) refl[k] += r[k] / amostras;
        }
        double cos = Math.abs(dy / n);
        double fres = 0.18 + 0.75 * Math.pow(1 - cos, 5);

        // Luz no chao em volta do carro + reflexo alongado da faixa de LED
        double poca = Math.exp(-sq(x / 2.8) - sq((z - 5.6) / 2.4));
        double estria = Math.exp(-sq(x / 1.5)) * Math.exp(-(ZW - z) / 2.2);

        double[] c = new double[3];
        for (int k = 0; k < 3; k++) {
            double base = P[k] * (0.22 + 0.10 * clampD(z / ZW, 0, 1)) * peca;
            c[k] = base + refl[k] * fres + (P[k] * 0.8 + L[k] * 0.20) * poca * 0.75 * FORCA
                 + L[k] * estria * 0.40 * FORCA;
            c[k] *= (1 - 0.45 * rejunte);
        }
        return c;
    }

    // ------------------------------------------------------------------ utilidades

    /** Converte para 0..255 com curva suave nos brilhos + ruido fino (evita faixas). */
    static int cor(double[] c, java.util.Random rnd) {
        int[] o = new int[3];
        double r = (rnd.nextDouble() + rnd.nextDouble() - 1) * 2.2;
        for (int k = 0; k < 3; k++) {
            double x = c[k];
            double t = x < 0.8 ? x : 0.8 + 0.2 * (1 - Math.exp(-(x - 0.8) / 0.2));
            o[k] = clamp((int) Math.round(t * 255 + r), 0, 255);
        }
        return (o[0] << 16) | (o[1] << 8) | o[2];
    }

    /** Ruido suave 2D (value noise) para a textura de concreto. */
    static double ruido(double x, double y) {
        int ix = (int) Math.floor(x), iy = (int) Math.floor(y);
        double fx = x - ix, fy = y - iy;
        fx = fx * fx * (3 - 2 * fx); fy = fy * fy * (3 - 2 * fy);
        double a = hash(ix * 92821 ^ iy * 68917), b = hash((ix + 1) * 92821 ^ iy * 68917);
        double c = hash(ix * 92821 ^ (iy + 1) * 68917), d = hash((ix + 1) * 92821 ^ (iy + 1) * 68917);
        return a + (b - a) * fx + (c - a) * fy + (a - b - c + d) * fx * fy;
    }

    /** Faixa de meia-largura [meia] (em metros) com borda suave de [suav]. */
    static double cobreY(double dist, double meia, double suav) {
        return 1 - suave(meia, meia + suav, dist);
    }

    /** Quanto um traco de meia-largura [meia] cobre o pixel a distancia [dist]. */
    static double cobre(double dist, double meia, double px) {
        return clampD((meia - dist) / Math.max(px, 1e-9) + 0.5, 0, 1);
    }

    static double hash(int n) {
        long x = n * 0x9E3779B97F4A7C15L;
        x ^= x >>> 31; x *= 0xBF58476D1CE4E5B9L; x ^= x >>> 29;
        return (x >>> 11) / (double) (1L << 53);
    }

    static double suave(double a, double b, double x) {
        double t = clampD((x - a) / (b - a), 0, 1);
        return t * t * (3 - 2 * t);
    }

    static double sq(double x) { return x * x; }
    static double frac(double x) { return x - Math.floor(x); }
    static int clamp(int x, int a, int b) { return Math.max(a, Math.min(b, x)); }
    static double clampD(double x, double a, double b) { return Math.max(a, Math.min(b, x)); }
    static double[] div(double[] v, double d) { return new double[]{v[0] / d, v[1] / d, v[2] / d}; }

    static void salvarJpg(BufferedImage img, File f, float qualidade) throws Exception {
        Iterator<ImageWriter> it = ImageIO.getImageWritersByFormatName("jpg");
        ImageWriter w = it.next();
        ImageWriteParam p = w.getDefaultWriteParam();
        p.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        p.setCompressionQuality(qualidade);
        try (ImageOutputStream out = ImageIO.createImageOutputStream(f)) {
            w.setOutput(out);
            w.write(null, new IIOImage(img, null, null), p);
        }
        w.dispose();
    }
}
