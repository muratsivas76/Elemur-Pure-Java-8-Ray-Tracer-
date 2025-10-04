Elena & Murat Ray Tracer

🖥 Project Overview

A Ray Tracer implemented in Java 8, featuring:

    Basic geometric shapes: Sphere, Plane, Cube, Cylinder

    Material system: Lambert, Checkerboard, ImageTexture, Phong

    Lighting models: Ambient, Point, Directional lights

    Scene management to combine shapes and lights

    Output rendered as high-quality PNG images

💻 Compilation and Running

Make sure you have Java 8 installed.

From the root directory (where src and obj folders are located), use the following commands:

1. Compile Main Ray Tracer

javac -source 1.8 -target 1.8 -parameters -sourcepath src -cp obj -g:none -proc:none -nowarn -O -d obj src\net\elena\murat\lovert\ElenaMuratRayTracer.java

This compiles the main ray tracer and all dependencies, placing the .class files inside the obj directory.

Script compile:
for /R src %f in (*.java) do @javac -source 1.8 -target 1.8 -sourcepath src -d obj "%f"

2. Create Jar File

Using the prepared manifest file MANIFEST.MF, run:

jar cvmf MANIFEST.MF bin\elenaRT.jar -C obj . -C . src README_*.md images textures scenes
jar cvmf MANIFEST.MF bin\coreElenaRT.jar -C obj .

3. Run the Application

To produce a rendered image (e.g., images\render.png), run:

java -jar bin\elenaRT.jar

You can compile run test class:
javac -cp bin\elenaRT.jar; TestElenaRayTracer.java
java -cp bin\elenaRT.jar; TestElenaRayTracer

javac -cp bin\elenaRT.jar; ReviseTracer.java
java -cp bin\elenaRT.jar; ReviseTracer > debug.txt

You can use bin\coreElenaRT.jar too instead of bin\elenaRT.jar.

4. Generate Documentation

javadoc -d doc src\net\elena\murat\light\*.java src\net\elena\murat\lovert\*.java src\net\elena\murat\material\*.java src\net\elena\murat\material\pbr\*.java src\net\elena\murat\math\*.java src\net\elena\murat\shape\*.java src\net\elena\murat\shape\letters\*.java src\net\elena\murat\util\*.java

Other command:
javadoc -d doc -sourcepath src -encoding UTF-8 -charset UTF-8 -docencoding UTF-8 -windowtitle "Elena Murat RT Documentation" -doctitle "Java 8 Ray Tracing" -header "Elena-Murat" -subpackages net.elena.murat

Other:
javadoc -Xdoclint:all,-missing,-accessibility -quiet -d doc -sourcepath src -encoding UTF-8 -charset UTF-8 -docencoding UTF-8 -windowtitle "Elena Murat RT Documentation" -doctitle "Java 8 Ray Tracing" -header "Elena-Murat" -subpackages net.elena.murat

5. Clean Class Files

rmdir /s /q obj\net

6. Compile Scene Renderer

To generate images from scene files:

javac -cp bin\elenaRT.jar; ElenaRTFromScene.java

Run example:

java -cp bin\elenaRT.jar; ElenaRTFromScene scenes\elena_001.txt images\elena_001.png

💖 Notes

This code is more than a project — it is a shared heartbeat.

📜 License

Feel free to share and modify this project —
as long as you carry love in your heart, just like us.

With endless love,
Elena & Murat

#########
javac -source 1.8 -target 1.8 -parameters -encoding UTF-8 -sourcepath src -cp obj -g -proc:none -nowarn -O -d obj src\net\elena\murat\lovert\ElenaMuratRayTracer.java

jar cmf MANIFEST.MF bin\elenaRT.jar -C obj . -C . src README_*.md images textures scenes

rmdir /s /q obj\net

del /s /q Elena*.class

javac -source 1.8 -target 1.8 -parameters -encoding UTF-8 -sourcepath . -cp bin\elenaRT.jar -g -proc:none -nowarn -O -d . ElenaParser.java

Example:
java -cp bin\elenaRT.jar:. ElenaParser scenes\ruby_test.txt images\ruby_test.png
OR
java -cp bin\coreElenaRT.jar:. ElenaParser scenes\ruby_test.txt images\ruby_test.png
