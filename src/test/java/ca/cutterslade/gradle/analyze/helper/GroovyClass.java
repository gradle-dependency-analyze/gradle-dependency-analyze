package ca.cutterslade.gradle.analyze.helper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class GroovyClass {
  private final String name;
  private final boolean annotation;
  private final Set<String> usedClasses = new HashSet<>();
  private final Set<ClassConstantUsage> usedClassConstants = new HashSet<>();
  private final Set<ClassConstant> classConstants = new HashSet<>();
  private final Set<ClassAnnotation> classAnnotations = new HashSet<>();

  public GroovyClass(String name) {
    this(name, false);
  }

  public GroovyClass(String name, boolean annotation) {
    this.annotation = annotation;
    this.name = name;
  }

  public GroovyClass withClassAnnotation(String annotationName, String value) {
    classAnnotations.add(new ClassAnnotation(annotationName, value));
    return this;
  }

  public GroovyClass usesClass(String clazz) {
    usedClasses.add(clazz);
    return this;
  }

  public GroovyClass addClassConstant(
      String constantName, String constantType, String constantValue, boolean isFinal) {
    classConstants.add(new ClassConstant(constantType, constantName, constantValue, isFinal));
    return this;
  }

  public GroovyClass addClassConstant(
      String constantName, String constantType, String constantValue) {
    return addClassConstant(constantName, constantType, constantValue, true);
  }

  public GroovyClass usesClassConstant(String clazz, String constantName, String constantType) {
    usedClassConstants.add(new ClassConstantUsage(clazz, constantName, constantType));
    return this;
  }

  private String buildUsedClasses() {
    StringBuilder out = new StringBuilder();
    for (String clazz : usedClasses) {
      String var = clazz.contains(".") ? clazz.substring(clazz.lastIndexOf('.') + 1) : clazz;
      out.append("  private ").append(clazz).append(" _").append(var.toLowerCase()).append("\n");
    }
    return out.toString();
  }

  private String buildUsedConstants() {
    StringBuilder out = new StringBuilder();
    for (ClassConstantUsage usedConst : usedClassConstants) {
      out.append("  private ")
          .append(usedConst.getType())
          .append(" _")
          .append(usedConst.getName().toLowerCase())
          .append(" = ")
          .append(usedConst.getClazz())
          .append(".")
          .append(usedConst.getName())
          .append("\n");
    }
    return out.toString();
  }

  private String buildClassConstants() {
    StringBuilder out = new StringBuilder();
    for (ClassConstant classConst : classConstants) {
      out.append("  public static ")
          .append(classConst.isFinal() ? "final " : "")
          .append(classConst.getType())
          .append(" ")
          .append(classConst.getName())
          .append(" = ")
          .append(classConst.getValue())
          .append("\n");
    }
    return out.toString();
  }

  private String buildClassAnnotation() {
    StringBuilder out = new StringBuilder();
    for (ClassAnnotation annotation : classAnnotations) {
      out.append("@")
          .append(annotation.getName())
          .append("(")
          .append(annotation.getValue())
          .append(")\n");
    }
    return out.toString();
  }

  public void create(File dir) {
    StringBuilder out = new StringBuilder();
    if (annotation) {
      out.append("import java.lang.annotation.ElementType\n");
      out.append("import java.lang.annotation.Retention\n");
      out.append("import java.lang.annotation.RetentionPolicy\n");
      out.append("import java.lang.annotation.Target\n\n");
      out.append("@Target(ElementType.TYPE)\n");
      out.append("@Retention(RetentionPolicy.RUNTIME)\n");
      out.append(buildClassAnnotation());
      out.append("public @interface ").append(name).append(" {\n");
      out.append("  String value()\n");
    } else {
      out.append(buildClassAnnotation());
      out.append("class ").append(name).append(" {\n");
      out.append(buildClassConstants());
      out.append(buildUsedClasses());
      out.append(buildUsedConstants());
    }
    out.append("}");

    try (FileWriter writer = new FileWriter(new File(dir, name + ".groovy"))) {
      writer.write(out.toString());
    } catch (IOException e) {
      throw new RuntimeException("Could not write class file: " + name + ".groovy", e);
    }
  }

  // Inner classes for type safety replacing Groovy Tuples

  private static class ClassConstantUsage {
    private final String clazz;
    private final String name;
    private final String type;

    public ClassConstantUsage(String clazz, String name, String type) {
      this.clazz = clazz;
      this.name = name;
      this.type = type;
    }

    public String getClazz() {
      return clazz;
    }

    public String getName() {
      return name;
    }

    public String getType() {
      return type;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      ClassConstantUsage that = (ClassConstantUsage) o;
      return Objects.equals(clazz, that.clazz)
          && Objects.equals(name, that.name)
          && Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
      return Objects.hash(clazz, name, type);
    }
  }

  private static class ClassConstant {
    private final String type;
    private final String name;
    private final String value;
    private final boolean isFinal;

    public ClassConstant(String type, String name, String value, boolean isFinal) {
      this.type = type;
      this.name = name;
      this.value = value;
      this.isFinal = isFinal;
    }

    public String getType() {
      return type;
    }

    public String getName() {
      return name;
    }

    public String getValue() {
      return value;
    }

    public boolean isFinal() {
      return isFinal;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      ClassConstant that = (ClassConstant) o;
      return isFinal == that.isFinal
          && Objects.equals(type, that.type)
          && Objects.equals(name, that.name)
          && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
      return Objects.hash(type, name, value, isFinal);
    }
  }

  private static class ClassAnnotation {
    private final String name;
    private final String value;

    public ClassAnnotation(String name, String value) {
      this.name = name;
      this.value = value;
    }

    public String getName() {
      return name;
    }

    public String getValue() {
      return value;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      ClassAnnotation that = (ClassAnnotation) o;
      return Objects.equals(name, that.name) && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, value);
    }
  }
}
