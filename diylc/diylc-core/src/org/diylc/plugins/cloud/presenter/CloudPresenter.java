package org.diylc.plugins.cloud.presenter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.List;

import org.apache.log4j.Logger;
import org.diylc.appframework.miscutils.ConfigurationManager;
import org.diylc.plugins.cloud.model.CommentEntity;
import org.diylc.plugins.cloud.model.IServiceAPI;
import org.diylc.plugins.cloud.model.ProjectEntity;
import org.diylc.plugins.cloud.model.UserEntity;

import com.diyfever.httpproxy.PhpFlatProxy;
import com.diyfever.httpproxy.ProxyFactory;

public class CloudPresenter {

  private static String USERNAME_KEY = "cloud.Username";
  private static String TOKEN_KEY = "cloud.token";

  private static String ERROR = "Error";

  private final static Logger LOG = Logger.getLogger(CloudPresenter.class);
  private static final Object SUCCESS = "Success";

  private IServiceAPI service;
  private String serviceUrl;
  private String machineId;
  private String[] categories;

  private CloudListener listener;

  private boolean loggedIn = false;

  public CloudPresenter(CloudListener listener) {
    this.listener = listener;
  }

  public IServiceAPI getService() {
    if (service == null) {
      serviceUrl =
          ConfigurationManager.getInstance().readString(IServiceAPI.URL_KEY, "http://www.diy-fever.com/diylc/api/v1");
      ProxyFactory factory = new ProxyFactory(new PhpFlatProxy());
      service = factory.createProxy(IServiceAPI.class, serviceUrl);
    }
    return service;
  }

  public boolean logIn(String username, String password) throws CloudException {
    LOG.info("Trying to login to cloud as " + username);

    String res;
    try {
      res = getService().login(username, password, getMachineId());
    } catch (Exception e) {
      throw new CloudException(e);
    }

    if (res == null || res.equals(ERROR)) {
      LOG.info("Login failed");
      return false;
    } else {
      LOG.info("Login success");
      ConfigurationManager.getInstance().writeValue(USERNAME_KEY, username);
      ConfigurationManager.getInstance().writeValue(TOKEN_KEY, res);
      listener.loggedIn();
      this.loggedIn = true;
      return true;
    }
  }

  public boolean tryLogInWithToken() throws CloudException {
    String username = ConfigurationManager.getInstance().readString(USERNAME_KEY, null);
    String token = ConfigurationManager.getInstance().readString(TOKEN_KEY, null);

    if (username != null && token != null) {
      LOG.info("Trying to login to cloud using a token as " + username);
      String res;
      try {
        res = getService().loginWithToken(username, token, getMachineId());
      } catch (Exception e) {
        throw new CloudException(e);
      }
      if (res == null || res.equals(ERROR)) {
        LOG.info("Login failed");
        return false;
      } else {
        LOG.info("Login success");
        listener.loggedIn();
        this.loggedIn = true;
        return true;
      }
    } else
      return false;
  }

  public void logOut() {
    LOG.info("Logged out");
    ConfigurationManager.getInstance().writeValue(TOKEN_KEY, null);
    this.loggedIn = false;
    listener.loggedOut();
  }

  public boolean isLoggedIn() {
    return loggedIn;
  }

  public String[] getCategories() throws CloudException {
    if (categories == null) {
      Object res;
      try {
        res = getService().getCategories();
        if (res != null && res.equals(ERROR))
          throw new CloudException("Could not fetch categories from the server.");
        if (res instanceof List<?>) {
          @SuppressWarnings("unchecked")
          List<String> cats = (List<String>) res;
          cats.add(0, "");
          categories = cats.toArray(new String[0]);
        } else {
          throw new CloudException("Unexpected server response received for category list: " + res.getClass().getName());
        }
      } catch (Exception e) {
        throw new CloudException(e);
      }
    }
    return categories;
  }

  public void uploadProject(String projectName, String category, String description, String keywords, String diylcVersion,
      File thumbnail, File project) throws IOException, CloudException {
    String username = ConfigurationManager.getInstance().readString(USERNAME_KEY, null);
    String token = ConfigurationManager.getInstance().readString(TOKEN_KEY, null);

    if (username == null || token == null)
      throw new CloudException("Login failed. Please try to login again.");

    try {
      String res =
          getService().uploadProject(username, token, getMachineId(), projectName, category, description, diylcVersion,
              keywords, thumbnail, project, null);
      if (!res.equals(SUCCESS))
        throw new CloudException(res);
    } catch (Exception e) {
      throw new CloudException(e);
    }
  }

  public void postComment(int projectId, String comment) throws CloudException {
    String username = ConfigurationManager.getInstance().readString(USERNAME_KEY, null);
    String token = ConfigurationManager.getInstance().readString(TOKEN_KEY, null);

    if (username == null || token == null)
      throw new CloudException("Login failed. Please try to login again.");

    try {
      String res = getService().postComment(username, token, getMachineId(), projectId, comment);
      if (!res.equals(SUCCESS))
        throw new CloudException(res);
    } catch (Exception e) {
      throw new CloudException(e);
    }
  }

  public void createUserAccount(String username, String password, String email, String website, String bio)
      throws CloudException {
    try {
      String res = getService().createUser(username, password, email, website, bio);
      if (res == null)
        throw new CloudException("Could not create user account.");
      if (!SUCCESS.equals(res))
        throw new CloudException(res);
    } catch (Exception e) {
      throw new CloudException(e);
    }
  }

  public UserEntity getUserDetails() throws CloudException {
    String username = ConfigurationManager.getInstance().readString(USERNAME_KEY, null);
    String token = ConfigurationManager.getInstance().readString(TOKEN_KEY, null);

    Object res;
    try {
      res = getService().getUserDetails(username, token, getMachineId());
    } catch (Exception e) {
      throw new CloudException(e);
    }
    if (res instanceof String)
      throw new CloudException(res.toString());
    if (res instanceof UserEntity)
      return (UserEntity) res;
    throw new CloudException("Unexpected server response received for category list: " + res.getClass().getName());
  }

  public String getCurrentUsername() {
    return ConfigurationManager.getInstance().readString(USERNAME_KEY, null);
  }

  public void updatePassword(String oldPassword, String newPassword) throws CloudException {
    String username = ConfigurationManager.getInstance().readString(USERNAME_KEY, null);

    String res;
    try {
      res = getService().updatePassword(username, oldPassword, newPassword);
    } catch (Exception e) {
      throw new CloudException(e);
    }
    if (!res.equals(SUCCESS))
      throw new CloudException(res);
  }

  public void updateUserDetails(String email, String website, String bio) throws CloudException {
    String username = ConfigurationManager.getInstance().readString(USERNAME_KEY, null);
    String token = ConfigurationManager.getInstance().readString(TOKEN_KEY, null);

    String res;
    try {
      res = getService().updateUserDetails(username, token, getMachineId(), email, website, bio);
    } catch (Exception e) {
      throw new CloudException(e);
    }
    if (!res.equals(SUCCESS))
      throw new CloudException(res);
  }

  public List<ProjectEntity> search(String criteria, String category, String sortOrder, int pageNumber, int itemsPerPage)
      throws CloudException {
    LOG.info(String.format("search(%1$s,%2$s,%3$s,%4$d,%5$d)", criteria, category, sortOrder, pageNumber, itemsPerPage));
    try {
      Object res = getService().search(criteria, category, pageNumber, itemsPerPage, sortOrder);
      // Thread.sleep(2000);
      if (res == null)
        throw new CloudException("Failed to retreive search results.");
      if (res instanceof String)
        throw new CloudException(res.toString());
      if (res instanceof List<?>) {
        @SuppressWarnings("unchecked")
        List<ProjectEntity> projects = (List<ProjectEntity>) res;
        LOG.info("Received " + projects.size() + " results. Downloading thumbnails...");
        // Download thumbnails and replace urls with local paths to speed up loading in the main
        // thread
        for (int i = 0; i < projects.size(); i++) {
          String url = projects.get(i).getThumbnailUrl();
          URL website = new URL(url);
          ReadableByteChannel rbc = Channels.newChannel(website.openStream());
          File temp = File.createTempFile("thumbnail", ".png");
          FileOutputStream fos = new FileOutputStream(temp);
          fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
          projects.get(i).setThumbnailUrl(temp.getAbsolutePath());
          fos.close();
        }
        LOG.info("Finished downloading thumbnails.");
        return projects;
      }
      throw new CloudException("Unexpected server response received for search results: " + res.getClass().getName());
    } catch (Exception e) {
      throw new CloudException(e);
    }
  }

  public String[] getSortings() throws CloudException {
    try {
      return getService().getSortings().toArray(new String[0]);
    } catch (Exception e) {
      throw new CloudException(e);
    }
  }

  public List<CommentEntity> getComments(int projectId) throws CloudException {
    try {
      Object res = getService().getComments(projectId);
      if (res == null)
        throw new CloudException("Failed to retreive search results.");
      if (res instanceof String)
        throw new CloudException(res.toString());
      if (res instanceof List<?>) {
        @SuppressWarnings("unchecked")
        List<CommentEntity> comments = (List<CommentEntity>) res;
        return comments;
      }
      throw new CloudException("Unexpected server response received for comments: " + res.getClass().getName());
    } catch (Exception e) {
      throw new CloudException(e);
    }
  }

  private String getMachineId() {
    if (machineId == null) {
      try {
        InetAddress ip = InetAddress.getLocalHost();

        NetworkInterface network = NetworkInterface.getByInetAddress(ip);

        byte[] mac = network.getHardwareAddress();

        StringBuilder sb = new StringBuilder(18);
        for (byte b : mac) {
          if (sb.length() > 0)
            sb.append(':');
          sb.append(String.format("%02x", b));
        }

        machineId = sb.toString();
      } catch (Exception e) {
        machineId = "Generic";
      }
    }
    return machineId;
  }
}
