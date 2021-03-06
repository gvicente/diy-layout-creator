package org.diylc.parsing;

import java.util.List;

import org.diylc.core.Project;
import org.w3c.dom.Element;

public class V2FileParser implements IOldFileParser {

//  private static final Logger LOG = Logger.getLogger(V2FileParser.class);

  @Override
  public boolean canParse(String version) {
    return version.equals("2.0");
  }

  @Override
  public Project parseFile(Element root, List<String> warnings) {
    Project project = new Project();
    warnings.add("V2 file parser is not yet implemented due to lack of interest and short life of DIYLC V2.");
    // String projectName = root.getAttribute("projectName");
    // String credits = root.getAttribute("credits");
    // String width = root.getAttribute("width");
    // String height = root.getAttribute("height");
    // NodeList childNodes = root.getChildNodes();
    // for (int i = 0; i < childNodes.getLength(); i++) {
    // Node node = childNodes.item(i);
    // if (node.getNodeType() == Node.ELEMENT_NODE) {
    // if (node.getNodeName().equalsIgnoreCase("component")) {
    // LOG.debug(node.getAttributes().getNamedItem("name").getNodeValue());
    // } else {
    // LOG.debug("Unrecognized node name found: " + node.getNodeName());
    // }
    // }
    // }
    return project;
  }

}
