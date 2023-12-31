package org.icepear.echarts;

import com.github.jknack.handlebars.internal.lang3.StringUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.icepear.echarts.render.Engine;
import org.icepear.echarts.snapshotSaver.Base64Saver;
import org.icepear.echarts.snapshotSaver.ImageSaver;
import org.icepear.echarts.snapshotSaver.SnapShotSaver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;

public class Snapshot {
    private static final String PHANTOMJS_HOME = "D:/works/java/Snapshot-PhantomJS/phantomjs/";
    private static final String PHANTOMJS_EXEC = PHANTOMJS_HOME + "phantomjs.exe";
    private static final String SCRIPT_NAME = PHANTOMJS_HOME + "generate-images.js";
    private static final String[] SUPPORTED_FILE_TYPES = new String[]{"png", "jpg"};
    private static Logger logger = LoggerFactory.getLogger(Snapshot.class);

    private static void writeStdin(String html, OutputStream stdin) {
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stdin));
            writer.write(html);
            writer.close();
        } catch (IOException e) {
            logger.error("Write Html into STDIN failed. " + e.getMessage());
        }
    }

    public static boolean checkPhantomJS() {
        try {
            Process p = new ProcessBuilder(PHANTOMJS_EXEC, "--version").start();
            String stdout = IOUtils.toString(p.getInputStream(), Charset.defaultCharset());
            logger.info("PhantomJS is installed and its version is " + stdout);
        } catch (Exception e) {
            logger.error("PhantomJS is not installed. You need to install it before proceeding." + e.getMessage());
            return false;
        }
        return true;
    }

    private static boolean isFileTypeSupported(String fileType) {
        for (String supportedType : SUPPORTED_FILE_TYPES) {
            if (supportedType.equals(fileType))
                return true;
        }
        return false;
    }

    private static String postProcess(String rawImageData) {
        String[] contentArray = rawImageData.split(",");
        if (contentArray.length != 2) {
            logger.error("Illegal raw image data.");
            return "";
        }
        return contentArray[1];
    }

    /**
     * @param settings
     * @return image data in Base64 string format
     */
    public static String takeSnapshot(SnapshotSettingsBuilder settings) {
        if (!isFileTypeSupported(settings.getFileType())) {
            logger.error("The file type you request is not supported.");
            return "";
        }
        if (!checkPhantomJS()) {
            return "";
        }
        if (settings.getChart() == null && settings.getOption() == null) {
            logger.error("Invalid snapshot settings. Empty chart and option.");
            return "";
        }
        logger.info("Generating files...");
        Option option = settings.getOption();
        Chart<?, ?> chart = settings.getChart();
        Engine engine = new Engine();
        String content = "";

        String html = (option == null) ? engine.renderHtml(chart) : engine.renderHtml(option);
        // 替换在线的js文件
        html = html.replaceAll("(?is)<script src.+?</script>", "<script src=\"file:///"+PHANTOMJS_HOME+"echarts.min.js\"></script>");
        try {
            URL res = Snapshot.class.getClassLoader().getResource(SCRIPT_NAME);
            String scriptPath = SCRIPT_NAME;
            Process p = new ProcessBuilder(PHANTOMJS_EXEC, scriptPath, settings.getFileType(),
                    settings.getDelay() * 1000 + "", settings.getPixelRatio() + "").start();
            writeStdin(html, p.getOutputStream());
            String errmsg = IOUtils.toString(p.getErrorStream(), Charset.defaultCharset());
            if (StringUtils.isNoneBlank(errmsg)) {
                System.out.println(errmsg);
                return null;
            }
            String txt = IOUtils.toString(p.getInputStream(), Charset.defaultCharset());
            File file = new File(PHANTOMJS_HOME + "output/" + StringUtils.strip(txt));
            content = FileUtils.readFileToString(file, Charset.defaultCharset());
            file.delete();
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

        return postProcess(content);
    }

    public static void saveSnapshot(String imageData, String path) {
        String[] pathParts = path.split("\\.");
        String suffix = pathParts[pathParts.length - 1];
        SnapShotSaver saver;
        if (suffix.equals("png") || suffix.equals("jpg")) {
            saver = new ImageSaver();
        } else {
            saver = new Base64Saver();
        }
        saver.save(imageData, path);
    }
}