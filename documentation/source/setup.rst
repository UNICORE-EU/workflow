Installing and setting up the UNICORE Workflow server
=====================================================

This chapter covers basic installation of the workflow server
and integration into an existing UNICORE system.

General UNICORE configuration concepts (such as gateway 
integration, shared registry, attribute sources) fully apply,
and you should refer to the UNICORE/X manual for details.

Prerequisites
-------------

 * Java 8 (JRE or SDK) or later
 * An existing UNICORE installation with Gateway, Registry 
   and one ore more UNICORE/X target systems
 * (for production use) A server certificate for the Workflow server

Updating from previous versions
-------------------------------

Please refer to the separate release notes!



Installation
------------

The workflow server is available either as a binary tgz bundle or as
Linux packages (deb or rpm).

The basic installation procedure is completely analogous to the
installation of the UNICORE core servers.

If you downloaded the workflow system bundle, either use the graphical
installer, or untar the tar.gz, edit configure.properties and run
configure.py

 * Graphical installer: during installation, you will be asked for 
   the parameters of your UNICORE installation.
 * Using the tar.gz bundle: please review the configure.properties file 
   and edit the parameters to integrate the workflow services into your 
   existing UNICORE environment. Then call +./configure.py+ to apply 
   your settings to the configuration files. Finally use +./install.py+ to 
   install the workflow server files to the selected installation directory.

If using the Linux packages, simply install using the package manager
of your system.

Setup
-----

After installation, there are some manual steps needed to integrate the 
new servers into your UNICORE installation.

 * Gateway: edit +gateway/conf/connections.properties+ and add the connection 
   data for the workflow server(s). For example::

   WORKFLOW = https://localhost:7700

 * XUUDB: if you chose to use an XUUDB for workflow and service orchestrator,
   you might have to add entries to the XUUDB to allow users
   access to the workflow engine. Optionally, you can edit the GCID used
   by the workflow/servorch servers, so that existing entries in the XUUDB
   will match.

 * Registry: if the registry is setup to use access control (which is the default),
   you need to allow the workflow and servorch services to register themselves in
   the Registry. The exact procedure depends on how you configured your Registry,
   please cross-reference the section "Enabling access control" in the Registry 
   manual. If you're using default certificates and the XUUDB, the required entries 
   can be added as follows::

   cd xuudb
   bin/admin.sh add REGISTRY <workflow>/conf/workflow.pem nobody server


Workflow data storage
---------------------

For storing workflow data (i.e. input/output files needed by the workflow tasks)
a storage service instance has to be available.

===== Storage Factory

This is the "best" way to store workflow data. Each workflow will
store its data on its own storage service instance, making management
of these data simpler. The client (UCC) allows to choose the
storage factory that should be used.

Verifying the installation
--------------------------

If you use the UNICORE Rich Client, you should see the workflow service in the Grid Browser view,
and you should be able to submit workflows to it.

Using the UNICORE commandline client, you can
check whether the new servers are available and accessible::

  ucc system-info -l


should include output such as::


   Checking for Workflow submission service ...
   ... OK, found 1 service(s)
     + https://localhost:8080/WORKFLOW/services/WorkflowFactory?res=default_workflow_submission


To check whether the services are accessible, you can use ::


  ucc rest get https://localhost:8080/WORKFLOW/rest/workflow/



Running a test job
++++++++++++++++++

Using UCC again, you can submit workflows::

  ucc workflow-submit /path/to/samples/date.json


RESTful API
+++++++++++

You can find an API reference and usage examples on the UNICORE wiki
at https://sourceforge.net/p/unicore/wiki/REST_API
