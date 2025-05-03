//Programa para procesar imagenes con filtros
//Haziel Lopez Castillo
//Vanesa Morales Gutiérrez

/*Imports*/
//Manipular imagenes
import java.awt.image.BufferedImage;
//In y out de archivos
import java.io.*;
//Acceso a la web
import java.net.URI;
import java.net.URL;
//Manejo de archivos
import java.nio.file.*;
//Colecciones
import java.util.*;
import java.util.concurrent.*;
//Leer y escribir imagenes
import javax.imageio.ImageIO;

/*Clase*/

public class ImageProcessor {
    //Solo es una variable para la carpeta de las imagenes
    private static final String OUTPUT_DIR = "Output-imagenes";


    //Aqui leemos las urls (Costo hacer funcionar esta parte)
    public static void main(String[] args) throws IOException, InterruptedException {
        List<String> urls = Files.readAllLines(Paths.get("urls.txt"));
        Map<String, String> urlMap = new ConcurrentHashMap<>();

        for (String url : urls) {
            url = url.trim();
            if (!url.isEmpty() && (url.startsWith("http://") || url.startsWith("https://"))) {
                urlMap.put(getImageName(url), url);
            }
        }

        //Creamos la carpeta de salida y aseguramos que existe (Habia un error que solo no encontraba)
        Files.createDirectories(Paths.get(OUTPUT_DIR));

        //Creamos la pool de hilos
        ExecutorService downloadExecutor = Executors.newFixedThreadPool(4);
        ExecutorService filterExecutor = Executors.newFixedThreadPool(4);
        ExecutorService saveExecutor = Executors.newFixedThreadPool(4);

        //Esto es para procesar cada URL
        for (String name : urlMap.keySet()) {
            downloadExecutor.submit(() -> {
                try {
                    BufferedImage original = ImageIO.read(new URL(urlMap.get(name)));

                    filterExecutor.submit(() -> applyAndSaveFilter(original, name, "sepia", saveExecutor));
                    filterExecutor.submit(() -> applyAndSaveFilter(original, name, "bw", saveExecutor));
                    filterExecutor.submit(() -> applyAndSaveFilter(original, name, "sharpen", saveExecutor));

                } catch (IOException e) {
                    System.err.println("Error descargando imagen: " + e.getMessage());
                }
            });
        }

        //Terminar el proceso
        downloadExecutor.shutdown();
        downloadExecutor.awaitTermination(10, TimeUnit.MINUTES);
        filterExecutor.shutdown();
        filterExecutor.awaitTermination(10, TimeUnit.MINUTES);
        saveExecutor.shutdown();
        saveExecutor.awaitTermination(10, TimeUnit.MINUTES);
    }

    //Aplicamos y guardamos
    private static void applyAndSaveFilter(BufferedImage original, String name, String filter,
                                           ExecutorService saveExecutor) {
        BufferedImage filtered = switch (filter) {
            case "sepia" -> applySepia(original);
            case "blk&wht" -> applyBlackAndWhite(original);
            case "sharpen" -> applySharpen(original);
            default -> null;
        };

        if (filtered != null) {
            saveExecutor.submit(() -> {
                try {
                    File output = new File(OUTPUT_DIR + "/" + name + "_" + filter + ".jpg");
                    ImageIO.write(filtered, "jpg", output);
                } catch (IOException e) {
                    System.err.println("Error al guardar la imagen: " + e.getMessage());
                }
            });
        }
    }

    //Esto es solo para los nombres de las imagenes
    private static String getImageName(String url) {
        try {
            return Paths.get(URI.create(url).getPath()).getFileName().toString().replaceAll("\\.[^.]+$", "");
        } catch (IllegalArgumentException e) {
            return "imagen";
        }
    }

    //Aplicar filtros
    //(Esta parte fue generada por IA, fue lo mas facil a solo buscarla en internet espero no sea un problema o_O)
    private static BufferedImage applySepia(BufferedImage img) {
        BufferedImage sepia = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int p = img.getRGB(x, y);
                int a = (p >> 24) & 0xff;
                int r = (p >> 16) & 0xff;
                int g = (p >> 8) & 0xff;
                int b = p & 0xff;

                int tr = (int)(0.393*r + 0.769*g + 0.189*b);
                int tg = (int)(0.349*r + 0.686*g + 0.168*b);
                int tb = (int)(0.272*r + 0.534*g + 0.131*b);

                r = Math.min(255, tr);
                g = Math.min(255, tg);
                b = Math.min(255, tb);

                sepia.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }
        return sepia;
    }

    private static BufferedImage applyBlackAndWhite(BufferedImage img) {
        BufferedImage bw = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        bw.getGraphics().drawImage(img, 0, 0, null);
        return bw;
    }

    private static BufferedImage applySharpen(BufferedImage img) {
        float[] sharpenKernel = {
                0.f, -1.f,  0.f,
                -1.f,  5.f, -1.f,
                0.f, -1.f,  0.f
        };
        BufferedImage result = new BufferedImage(img.getWidth(), img.getHeight(), img.getType());
        java.awt.image.Kernel kernel = new java.awt.image.Kernel(3, 3, sharpenKernel);
        java.awt.image.ConvolveOp op = new java.awt.image.ConvolveOp(kernel);
        op.filter(img, result);
        return result;
    }
}
