import java.io.*;
import java.util.*;

public class MultiLineConstructorExtractor {
    public static void main(String[] args) {
        String srcDir = "/home/muratsivas76/istasyon/java/elemurrt/src";
        String outFile = "/home/muratsivas76/istasyon/java/elemurrt/tool/constructors.txt";
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outFile))) {
            processJavaFiles(new File(srcDir), writer, srcDir);
            System.out.println("Extracted succesfully to tools/constructors.txt!");
        } catch (IOException e) {
            System.err.println("Hata: " + e.getMessage());
        }
    }

    private static void processJavaFiles(File dir, BufferedWriter writer, String basePath) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                processJavaFiles(file, writer, basePath);
            } else if (file.getName().endsWith(".java")) {
                extractConstructors(file, writer, basePath);
            }
        }
    }

    private static void extractConstructors(File javaFile, BufferedWriter writer, String basePath) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(javaFile));
        String className = javaFile.getName().replace(".java", "");
        String relativePath = javaFile.getParent().substring(basePath.length());
        StringBuilder constructor = new StringBuilder();
        boolean readingConstructor = false;
        int braceCount = 0;
        int parenthesisCount = 0;
        
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                // Check for constructor start
                if (!readingConstructor && line.startsWith("public " + className + "(")) {
                    readingConstructor = true;
                    constructor.append(line);
                    
                    // Count parentheses and braces
                    parenthesisCount += countOccurrences(line, '(') - countOccurrences(line, ')');
                    braceCount += countOccurrences(line, '{') - countOccurrences(line, '}');
                    
                    // If we already found the complete constructor signature
                    if (parenthesisCount == 0 && braceCount > 0) {
                        writeConstructor(writer, relativePath, javaFile.getName(), constructor.toString());
                        readingConstructor = false;
                        constructor.setLength(0);
                        braceCount = 0;
                    }
                    continue;
                }
                
                // If we're in the middle of reading a constructor
                if (readingConstructor) {
                    constructor.append(" ").append(line);
                    
                    // Update parenthesis and brace counts
                    parenthesisCount += countOccurrences(line, '(') - countOccurrences(line, ')');
                    braceCount += countOccurrences(line, '{') - countOccurrences(line, '}');
                    
                    // If we've found the complete constructor signature and opening brace
                    if (parenthesisCount == 0 && braceCount > 0) {
                        writeConstructor(writer, relativePath, javaFile.getName(), constructor.toString());
                        readingConstructor = false;
                        constructor.setLength(0);
                        braceCount = 0;
                    }
                }
            }
        } finally {
            reader.close();
        }
    }

    private static int countOccurrences(String str, char ch) {
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == ch) {
                count++;
            }
        }
        return count;
    }

    private static void writeConstructor(BufferedWriter writer, String relativePath, 
                                       String fileName, String constructor) throws IOException {
        // Clean up the constructor string
        String cleanConstructor = constructor.replaceAll("\\s+", " ")
                                           .replaceAll("\\s*,\\s*", ", ")
                                           .replaceAll("\\s*\\(\\s*", "(")
                                           .replaceAll("\\s*\\)\\s*", ")")
                                           .replaceAll("\\s*\\{\\s*", " {");
        
        // Extract just the signature (everything before the opening brace)
        int braceIndex = cleanConstructor.indexOf('{');
        if (braceIndex > 0) {
            cleanConstructor = cleanConstructor.substring(0, braceIndex).trim();
        }
        
        writer.write(relativePath + File.separator + fileName + ":");
        writer.newLine();
        writer.write(cleanConstructor + ";");
        writer.newLine();
        writer.newLine();
    }
	
}
