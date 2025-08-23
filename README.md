## office-app-api



# Steps to Fix Font Issue in JasperReports (iReport 6.x)

1.	Install the Font Extension

    •	Download the font you want (e.g., Arial.ttf or Unicode-safe font like DejaVu Sans).
    •	Open iReport Designer.
    •	Go to Settings → Jaspersoft Studio → Fonts (sometimes called Fonts → Install Font).

2.	Configure Font in iReport

	•	After adding, you will see an option to:
	•	Check “PDF Embedded” ✅
	•	Choose encoding → select Identity-H (important for Unicode).
	•	Save it.

3.	Export Font to JAR

	•	When you add the font in iReport, it creates a font extension JAR (usually xxx-fonts.jar).
	•	Find this JAR in your iReport installation directory (or in ~/.jasperreports/fonts).

4.	Add Font JAR to Spring Boot Project

	•	Copy the generated font JAR into your project’s src/main/resources or add it as a dependency in your build.
	•	If using Maven, install the JAR into your local repo:

'''
mvn install:install-file \
-Dfile=/Users/sainzayabatkhuu/Desktop/gitprojects/office-app-api/data/OopsAF-fonts.jar \
-DgroupId=com.oopsAF \
-DartifactId=jasper-fonts \
-Dversion=1.0 \
-Dpackaging=jar
'''

Then add in pom.xml:

<dependency>
  <groupId>com.oopsAF</groupId>
  <artifactId>jasper-fonts</artifactId>
  <version>1.0</version>
</dependency>