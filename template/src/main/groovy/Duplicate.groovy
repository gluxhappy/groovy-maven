import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes

static def copy(Path source, Path dest) throws IOException {
    if (!Files.exists(source)) {
        throw new IllegalStateException("Source " + source + " does not exist.")
    }

    if (Files.isDirectory(source)) {
        Files.createDirectories(dest) // Create the destination directory if it does not exist
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path targetPath = dest.resolve(source.relativize(dir))
                if (!Files.exists(targetPath)) {
                    Files.createDirectory(targetPath)
                }
                return FileVisitResult.CONTINUE
            }

            @Override
            FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, dest.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING)
                return FileVisitResult.CONTINUE
            }
        })
    } else {
        Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING)
    }
}

String newModuleName=System.getProperty("example")?.strip()

if(!newModuleName) {
    Scanner scanner = new Scanner(System.in);
    System.out.print("Enter the name of the new module: ");
    newModuleName = scanner.nextLine().strip()
}

Path root;
if (Files.exists(Path.of("template"))) {
    root = Path.of(".")
} else if (Files.exists(Path.of("../template"))) {
    root = Path.of("..")
} else if(Files.exists(Path.of("../../../../template"))){
    root = Path.of("../../../..")
} else {
    throw new IllegalStateException("Unable to locate the project.")
}


Path source = root.resolve("template");
Path dest = root.resolve(newModuleName);

copy(source, dest)

Files.list(dest.resolve("src/main/groovy")).each {
    if(it.fileName.toString() != "GroovyMain.groovy") {
        Files.delete(it)
    }
}
Files.list(dest.resolve("src/main/java")).each {
    if(it.fileName.toString() != "JavaMain.java") {
        Files.delete(it)
    }
}

dest.resolve("pom.xml").with { pom ->
    Files.writeString(
            pom,
            Files.readString(pom)
                    .replace("groovy-maven-template",newModuleName)
                    .replaceAll("Duplicate.groovy", "GroovyMain.groovy")
    )
}

root.resolve("pom.xml").with { pom ->
    Files.writeString(
            pom,
            Files.readString(pom).replaceAll(
                    "</modules>",
                    "    <module>${newModuleName}</module>\n    </modules>"
            )
    )
}


