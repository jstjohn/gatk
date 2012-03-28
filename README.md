# Green lab fork of GATK
## Installation

1. Follow the steps on Broad's [GATK building Queue from source](http://www.broadinstitute.org/gsa/wiki/index.php/Queue#Building_Queue_from_source "Queue#Building_Queue_from_source") wiki.
2. Follow the steps for [setting up InteliJ Idea](http://www.broadinstitute.org/gsa/wiki/index.php/Queue_with_IntelliJ_IDEA "Setup InteliJ Idea") to work with Sting/queue.

### Installation issues
One gotcha I ran into is with the following InteliJ step:
>Back in the Project Structure window, under the Module 'Sting' , on the Sources tab make sure the following folders are selected

I found that the previous step which said to uncheck all but the `java/src` directory resulted in only that directly being listed as a source directory. The fix here is when you do this step to "make sure" these folders are selected, just navigate to the folders on the right side of the screen, right click on them, and select `source` or `test` from the dropdown. This adds the required files to the IDE.