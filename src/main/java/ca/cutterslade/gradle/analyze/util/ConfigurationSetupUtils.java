package ca.cutterslade.gradle.analyze.util;

import java.util.ArrayList;
import java.util.List;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.provider.Provider;

public final class ConfigurationSetupUtils {
  private ConfigurationSetupUtils() {}

  /**
   * Creates a list containing a single provider of the given configuration. Used for passing
   * configuration providers to tasks.
   *
   * @param configProvider The configuration provider
   * @return A list containing the provider
   */
  public static List<Provider<Configuration>> wrapInList(
      final Provider<Configuration> configProvider) {
    final List<Provider<Configuration>> list = new ArrayList<>();
    list.add(configProvider);
    return list;
  }

  /**
   * Sets up the configuration hierarchies between helper configurations and their parent
   * configurations. This should be called as early as possible in the configuration phase, not in
   * afterEvaluate.
   *
   * @param project The project
   * @param helperConfigName The name of the helper configuration
   * @param parentConfigName The name of the parent configuration it should extend from
   */
  public static void setupConfigurationHierarchy(
      final Project project, final String helperConfigName, final String parentConfigName) {
    if (project.getConfigurations().getNames().contains(helperConfigName)
        && project.getConfigurations().getNames().contains(parentConfigName)) {

      final Configuration helperConfig = project.getConfigurations().getByName(helperConfigName);
      final Configuration parentConfig = project.getConfigurations().getByName(parentConfigName);

      helperConfig.extendsFrom(parentConfig);
    }
  }
}
