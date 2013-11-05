Building the Project
	Dependencies

		    Apache Maven 3.0.4 or later

Targets

	$ mvn -Dmaven.test.skip=true clean install

Installing Plugin Locally

	Build the project to produce target/reports.hpi
	Remove any installation of the reports plugin in $jenkins.home/.jenkins/plugins/
	Copy target/build-pipeline-plugin.hpi to $jenkins.home/.jenkins/plugins/
	Start/Restart Jenkins
		OR
	Use Jenkins->Manage->Plugins->Advanced->Upload Plugin user interface.

PURPOSE:
	To provide a job overview page.
	The current job screen shows job names and assigned nodes, but not when the job is a farm job.
		Because of multiple entries per farm job, it would not be useful to add this column.
	The current job screen does not show an SCM URI column.
		Because of the possibility of multiple entries per job, it would not be useful to add this column.

	This plugin intends to provide this and related functionality.

	Because both columns can be sorted it is very easy to check for duplicate entries when there should not be any.
		For example: SCM URI

