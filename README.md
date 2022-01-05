# AgreementMakerLight (AML)

AgreementMakerLight is an automated and efficient ontology matching system. It has a flexible and extensible framework and is primarily based on the use of element-level matching techniques supported by background knowledge. AgreementMakerLight has been very successful in the OAEI competition, ranking first in F-measure in the several tracks throughout the years including: Anatomy, Conference, Multifarm, Library, Interactive Matching Evaluation, and Large Biomedical Ontologies.
AgreementMakerLight has been used by several institutions, including NASA, the Janssen Pharmaceutical Companies of Johnson & Johnson, and in the Global Agricultural Concept Scheme (GACS) from the Food and Agriculture Organization of the United Nations.


## Installing AML

AML comes in a ready-to-run jar file, which requires no installation.

You can just download the latest release from the releases tab (https://github.com/AgreementMakerLight/AML-Project/releases), extract the zip file, and AML will be ready to run.

You do need Java installed in your machine. The latest AML release has been tested in Oracle Java 1.7, 1.8 and 1.9. We can not guarantee it will run in Open JDK version of Java.


## Running AML in user interface mode

To run AML's user interface, you just have to execute the AgreementMakerLight.jar with no command line arguments.
This can be done by double-clicking the file (it if has execution permissions) or by typing:

    java -jar AgreementMakerLight.jar


## Running AML in command line mode

To run AML in command line mode, you have to execute the AgreementMakerLight.jar with command line arguments:
    
    java -jar AgreementMakerLight.jar OPTIONS

Where the OPTIONS are:

    -s (--source) 'path_to_source_ontology'     Specifies the source ontology file (mandatory)
    -t (--target) 'path_to_target_ontology'     Specifies the target ontology file (mandatory)
    -i (--input) 'path_to_input_alignment'      Specifies the input alignment file (mandatory in repair mode; used as reference in match mode)
    -o (--output) 'path_to_ouput_alignment'     Specifies the output alignment file (AML will not save the result without this)
    -a (--auto)                                 Sets AML in automatic match mode (one of the three modes must be specified)
    -m (--manual)                               Sets AML in manual match mode (you can configure it in the store/config.ini file)      
    -r (--repair)                               Sets AML in repair alignment mode (requires input alignment to repair)


## Building AML with Maven

Instead of downloading the latest AML release, you can use Maven to build it, after you clone the AML-Project repository.
Use the following command:

    mvn install

Note: We have found a few errors when compiling AML with Maven. We are working on fixing them, but please avoid this solution for the time being, and try downloading the latest AML release instead.

## Acknowledgements

AML is partially funded by the Portuguese FCT through the LASIGE Research Unit, and also by the SMILAX project (PTDC/EEI-ESS/4633/2014).

## Copyright 2013-2020 LASIGE

AML includes software developed at LASIGE (https://www.lasige.di.fc.ul.pt/) in collaboration with the ADVIS Lab (http://www.cs.uic.edu/Advis) and the Bioinformatics Unit of the IGC (http://bioinformatics.igc.gulbenkian.pt/ubi/).

If you use AML, please cite the following publication:

    D. Faria, C. Pesquita, E. Santos, M. Palmonari, I. Cruz, and F. Couto, The AgreementMakerLight ontology matching system, ODBASE 2013.
