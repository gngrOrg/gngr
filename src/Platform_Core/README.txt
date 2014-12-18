This project module is built into the main platform 
executable. It is a relatively compact core. The rest 
of the browser/platform is made of plugins. The
module Primary_Extension contains all the essential
browser functionality.

This module is released under the GPL license.

These are the *VM* arguments currently (5/20/2007) needed
to run Lobo from source (in Eclipse at least):

# New
-Dext.dirs=../XAMJ_Build/ext  -Dext.files=../HTML_Renderer/bin,../Primary_Extension/bin  -Djava.security.debug=denied

# Original
-Dext.dirs=/opt/XAMJ_Project/XAMJ_Build/ext -Dext.files=/opt/XAMJ_Project/Primary_Extension,/opt/XAMJ_Project/Common,/opt/XAMJ_Project/HTML_Renderer,/opt/XAMJ_Project/cssparser/bin,/opt/XAMJ_Project/JWebContentExtension

The following are recommended *program* arguments to run
from source:

-debug

The entry point class is org.lobobrowser.main.EntryPoint.

----------------------------------------------------------------
NOTE: All Lobo windows should be closed before launching the
browser, particularly after you've made code changes. EntryPoint 
will try to reuse any running instances.
----------------------------------------------------------------
