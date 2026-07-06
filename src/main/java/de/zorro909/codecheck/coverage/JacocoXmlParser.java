package de.zorro909.codecheck.coverage;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class JacocoXmlParser {

    public CoverageSnapshot parse(Path report) {
        try {
            Element root = DocumentBuilderFactory.newInstance()
                                                 .newDocumentBuilder()
                                                 .parse(report.toFile())
                                                 .getDocumentElement();
            NodeList classes = root.getElementsByTagName("class");
            Map<String, ClassCoverage> coverage = new HashMap<>();
            for (int i = 0; i < classes.getLength(); i++) {
                Element classElement = (Element) classes.item(i);
                String className = classElement.getAttribute("name");
                coverage.put(className, new ClassCoverage(
                        className, counter(classElement, "LINE"), counter(classElement, "BRANCH")));
            }
            return new CoverageSnapshot(coverage);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to parse JaCoCo report " + report, e);
        }
    }

    private CoverageMetric counter(Element classElement, String type) {
        NodeList counters = classElement.getElementsByTagName("counter");
        for (int i = 0; i < counters.getLength(); i++) {
            Element counter = (Element) counters.item(i);
            if (type.equals(counter.getAttribute("type"))) {
                return new CoverageMetric(Integer.parseInt(counter.getAttribute("missed")),
                                          Integer.parseInt(counter.getAttribute("covered")));
            }
        }
        return new CoverageMetric(0, 0);
    }
}
