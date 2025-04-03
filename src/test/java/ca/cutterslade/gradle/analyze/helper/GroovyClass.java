package ca.cutterslade.gradle.analyze.helper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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

  public GroovyClass(final String name) {
    this(name, false);
  }

  public GroovyClass(final String name, final boolean annotation) {
    this.annotation = annotation;
    this.name = name;
  }

  public GroovyClass withClassAnnotation(final String annotationName, final String value) {
    classAnnotations.add(new ClassAnnotation(annotationName, value));
    return this;
  }

  public GroovyClass usesClass(final String clazz) {
    usedClasses.add(clazz);
    return this;
  }

  public GroovyClass addClassConstant(
      final String constantName,
      final String constantType,
      final String constantValue,
      final boolean isFinal) {
    classConstants.add(new ClassConstant(constantType, constantName, constantValue, isFinal));
    return this;
  }

  public GroovyClass addClassConstant(
      final String constantName, final String constantType, final String constantValue) {
    return addClassConstant(constantName, constantType, constantValue, true);
  }

  public GroovyClass usesClassConstant(
      final String clazz, final String constantName, final String constantType) {
    usedClassConstants.add(new ClassConstantUsage(clazz, constantName, constantType));
    return this;
  }

  private String buildUsedClasses() {
    final StringBuilder out = new StringBuilder();
    for (final String clazz : usedClasses) {
      final String var = clazz.contains(".") ? clazz.substring(clazz.lastIndexOf('.') + 1) : clazz;
      out.append("  private ").append(clazz).append(" _").append(var.toLowerCase()).append("\n");
    }
    return out.toString();
  }

  private String buildUsedConstants() {
    final StringBuilder out = new StringBuilder();
    for (final ClassConstantUsage usedConst : usedClassConstants) {
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
    final StringBuilder out = new StringBuilder();
    for (final ClassConstant classConst : classConstants) {
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
    final StringBuilder out = new StringBuilder();
    for (final ClassAnnotation annotation : classAnnotations) {
      out.append("@")
          .append(annotation.getName())
          .append("(")
          .append(annotation.getValue())
          .append(")\n");
    }
    return out.toString();
  }

  public void create(final Path dir) throws IOException {
    final StringBuilder out = new StringBuilder();
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

    Files.write(dir.resolve(name + ".groovy"), out.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static class ClassConstantUsage {
    private final String clazz;
    private final String name;
    private final String type;

    public ClassConstantUsage(final String clazz, final String name, final String type) {
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
    public boolean equals(final Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      final ClassConstantUsage that = (ClassConstantUsage) o;
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

    public ClassConstant(
        final String type, final String name, final String value, final boolean isFinal) {
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
    public boolean equals(final Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      final ClassConstant that = (ClassConstant) o;
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

    public ClassAnnotation(final String name, final String value) {
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
    public boolean equals(final Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      final ClassAnnotation that = (ClassAnnotation) o;
      return Objects.equals(name, that.name) && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, value);
    }
  }
}
