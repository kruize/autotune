# Fork and clone the Autotune Repository

You must fork and set up the Autotune repository on your workstation so that you can create PRs and contribute. These steps must only be performed during initial setup.

- Fork the https://github.com/kruize/autotune repository into your GitHub account from the GitHub UI. You can do this by clicking on Fork in the upper right-hand corner.

- In the terminal on your workstation, change into the directory where you want to clone the forked repository.

- Clone the forked repository onto your workstation with the following command, replacing <user_name> with your actual GitHub username.
```
$ git clone git@github.com:<user_name>/autotune.git
```
- Change into the directory for the local repository you just cloned.
```
$ cd autotune
```
- Add an upstream pointer back to the OpenShift’s remote repository, in this case openshift-docs.
```
$ git remote add upstream git@github.com:kruize/autotune.git
```

