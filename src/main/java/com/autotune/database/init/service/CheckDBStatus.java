package com.autotune.database.init.service;

import com.autotune.database.init.KruizeHibernateUtil;
import com.autotune.operator.KruizeDeploymentInfo;
import org.hibernate.Session;
import org.hibernate.query.Query;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.CHARACTER_ENCODING;
import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.JSON_CONTENT_TYPE;

@WebServlet(asyncSupported = true)
public class CheckDBStatus extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType(JSON_CONTENT_TYPE);
        response.setCharacterEncoding(CHARACTER_ENCODING);
        if (null == KruizeHibernateUtil.getSessionFactory()) {
            response.setStatus(HttpServletResponse.SC_EXPECTATION_FAILED);
            response.getWriter().println("DOWN");
        } else {
            Session session = null;
            try {
                if (KruizeDeploymentInfo.isSaveToDB()) {
                    session = KruizeHibernateUtil.getSessionFactory().openSession();
                    Query query = session.createQuery("SELECT 1", null);
                    query.uniqueResult();
                }
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().println("UP");
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_EXPECTATION_FAILED);
                response.getWriter().println("DOWN");
            } finally {
                if (null != session) session.close();
            }
        }
        response.getWriter().close();
    }
}
