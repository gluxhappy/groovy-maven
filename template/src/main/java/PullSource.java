import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class PullSource {

    private List<String> escapeGroupIdPrefixes = List.of(
            "org.apache.maven",
            "org.codehaus.plexus",
            "org.sonatype");

    private String root;

    private LinkedList<String> context = new LinkedList<>();

    private List<Artifact> artifacts = new ArrayList<>();

    public static void main(String[] args) throws IOException, InterruptedException {
        for (String s : List.of(
                "mvn clean install",
                "mvn dependency:tree")) {
            runMaven(s, true);
        }
        for (String s : List.of(
                "mvn site:site",
                "mvn deploy:deploy",
                "mvn antrun:antrun",
                "mvn assembly:assembly",
                "mvn release:release")) {
            runMaven(s, false);
        }
        PullSource p = new PullSource();
        p.root = ".m2/repository";
        p.visit();
        for (Artifact artifact : p.artifacts) {
            String command =
                    ("mvn dependency:get -DgroupId=%s -DartifactId=%s -Dversion=%s " +
                            "-Dclassifier=sources -Dpackaging=jar -Dtransitive=false").formatted(artifact.groupId, artifact.artifactId, artifact.version);

            System.out.println(command);
            runMaven(command, false);
        }
    }

    public static void runMaven(String command, boolean needSuccess) throws IOException, InterruptedException {
        System.out.println("Executing: " + command);
        ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
        pb.inheritIO();
        int rt = pb.start().waitFor();
        if (needSuccess && rt != 0) {
            throw new IllegalStateException("Command exited with non-zero, return " + rt + ":" + command);
        }
    }


    public void visit() throws IOException {
        String current = getCurrent();
        if (new File(current + "/_remote.repositories").exists()) {
            StringBuilder groupIdBuilder = new StringBuilder(128);
            for (int i = 0; i < context.size() - 2; i++) {
                if (!groupIdBuilder.isEmpty()) {
                    groupIdBuilder.append(".");
                }
                groupIdBuilder.append(context.get(i));
            }
            String groupId = groupIdBuilder.toString();
            String artifactId = context.get(context.size() - 2);
            String version = context.get(context.size() - 1);
            for (String escapeGroupIdPrefix : escapeGroupIdPrefixes) {
                if (groupId.startsWith(escapeGroupIdPrefix)) {
                    return;
                }
            }
            if (!new File(current + "/" + artifactId + "-" + version + ".jar").exists()) {
                return;
            }
            if (new File(current + "/" + artifactId + "-" + version + "-sources.jar").exists()) {
                return;
            }
            artifacts.add(new Artifact(groupIdBuilder.toString(), artifactId, version));
            return;
        }

        String[] children = new File(current).list();
        if (children != null) {
            for (String f : children) {
                context.addLast(f);
                visit();
                context.removeLast();
            }
        }
    }

    public String getCurrent() {
        if (context.isEmpty()) {
            return root;
        } else {
            return root + "/" + String.join("/", context);
        }
    }

    public static class Artifact {
        public String groupId;
        public String artifactId;
        public String version;

        public Artifact(String groupId, String artifactId, String version) {
            this.groupId = groupId;
            this.version = version;
            this.artifactId = artifactId;
        }

        @Override
        public String toString() {
            return groupId + "/" + artifactId + "/" + version;
        }
    }
}
