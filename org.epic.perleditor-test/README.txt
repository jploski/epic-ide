EPIC Automated Test Suites
--------------------------

The pseudo-plug-in org.epic.perleditor-test contains two test suites for
org.epic.perleditor:

1. Tests defined in the source folder "src" are plain old unit tests for
   Eclipse-neutral code. They can be run using the JUnit run configuration.
2. Tests defined in the source folder "src-pde" are end-to-end tests that
   execute in a self-hosted Eclipse workbench. They can be run using
   the PDE JUnit run configuration.

Both test suites should be run with the following VM arguments:
   -Xmx256M -ea -Dorg.epic.perleditor-test.properties=<path to test.properties>

Moreover, the tests from "src" should be run with location of
org.epic.perleditor-test as their working directory.

The file test.properties contains local configuration settings and has to be
updated to match your environment. A sample version of this file is kept in CVS
for reference.

The folder test.in contains resources needed by tests (such as expected test
results).

The tests from "src-pde" require a preconfigured workspace as their fixture.
This workspace is kept in CVS (org.epic.perleditor-test/workspace). The path
to this workspace must be specified in the PDE JUnit run configuration.
While performing this step, do not forget to uncheck the "Clear workspace
data before launching" checkbox or the workspace directory will be destroyed!
The test workbench is launched with only EPIC plug-ins (and their dependencies).

(Some of) the "src-pde" tests simulate user input through Display.postEvent
and are therefore fragile: they will fail if you interact with the GUI while
they are running. Otherwise, they should run clean.

If you wish to test using mock objects, the EasyMock library is included
in the plug-in's classpath.

Last but not least, the present test suites are very incomplete.
Keep this in mind when making decisions based on their results.

Questions? -> jploski@users.sourceforge.net