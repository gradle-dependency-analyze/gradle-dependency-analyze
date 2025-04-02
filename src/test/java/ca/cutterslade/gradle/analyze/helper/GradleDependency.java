package ca.cutterslade.gradle.analyze.helper;

public class GradleDependency {
  private String configuration;
  private String id;
  private String reference;
  private String project;

  public GradleDependency setConfiguration(String configuration) {
    this.configuration = configuration;
    return this;
  }

  public GradleDependency setId(String id) {
    this.id = id;
    return this;
  }

  public GradleDependency setReference(String reference) {
    this.reference = reference;
    return this;
  }

  public GradleDependency setProject(String project) {
    this.project = project;
    return this;
  }

  private String getRightPart() {
    if (id != null) {
      return "'" + id + "'";
    }
    if (reference != null) {
      return reference;
    }
    if (project != null) {
      return "project(':" + project + "')";
    }
    return "";
  }

  public String get() {
    return configuration + "(" + getRightPart() + ")";
  }
}
