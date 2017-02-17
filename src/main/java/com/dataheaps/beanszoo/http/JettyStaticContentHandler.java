package com.dataheaps.beanszoo.http;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by matteopelati on 30/11/15.
 */
public class JettyStaticContentHandler extends AbstractHandler {

    final static String CONTENT_LENGTH = "Content-Length";
    final static String CONTENT_TYPE = "Content-Type";

    @Getter @Setter String rootLocalPath;
    @Getter @Setter Map<String,String> fileTypes = new HashMap<>();
    @Getter @Setter Map<String,String> rewriteRules = new HashMap<>();
    @Getter @Setter Map<String,String> headers = new HashMap<>();

    Path rootLocal;

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        rootLocal = Paths.get(rootLocalPath).toAbsolutePath().normalize();
    }

    @Override
    public void handle(String s, Request request, HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {

        if (req.getMethod().equals("GET")) {

            String requestPath = req.getPathInfo().toString();
            if (rewriteRules.containsKey(requestPath))
                requestPath = rewriteRules.get(req.getPathInfo().toString());

            Path reqPath = Paths.get(requestPath);
            Path absLocalPath = Paths.get(rootLocal.toString(), reqPath.toString());

            File f = absLocalPath.toFile();
            if (!f.exists() || f.isDirectory()) return;

            String ext = f.getName().substring(f.getName().lastIndexOf('.') + 1);

            FileInputStream i = new FileInputStream(f);
            resp.addHeader(CONTENT_LENGTH, Long.toString(f.length()));
            resp.addHeader(CONTENT_TYPE, fileTypes.containsKey(ext) ? fileTypes.get(ext) : "application/octect-stream");
            for (Map.Entry<String, String> e : headers.entrySet())
                resp.addHeader(e.getKey(), e.getValue());
            IOUtils.copy(i, resp.getOutputStream());
            resp.getOutputStream().flush();
            resp.getOutputStream().close();

        }
    }
}
