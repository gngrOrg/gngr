# Contributing to gngr

We are glad you want to contribute to `gngr`! This document will help answer common questions you may have during your first contribution.

### Testing and reporting issues
This is the simplest way of contributing to `gngr`: use it and when you find bugs shoot us an issue report on GitHub.

### Triaging
When an issue is reported by a user, we need help in diagnosing the root cause and breaking it down to a small
test-case. This often requires familiarity with the browser stack (HTTP, HTML, CSS, JS, etc), while knowledge of
`gngr`'s implementation is usually not required.

To help with triaging, [search for issues with the `need-triage` label](https://github.com/UprootLabs/gngr/labels/need-triage).

### Advice
We often need advice / guidance from experts in browser and related technologies.
Please [search for issues with the `need-advice` label](https://github.com/UprootLabs/gngr/labels/need-advice).

### Code

We have a 3 step process for contributions:

* Commit changes to a git branch, making sure to sign-off those changes for the [Developer Certificate of Origin](#developer-certification-of-origin-dco).
* Create a Github Pull Request for your change, following the instructions in the pull request template.
* Perform a Code Review with the project maintainers on the pull request.


Issues that can be easily picked up are marked with the
following tags:
* [need-code](https://github.com/UprootLabs/gngr/labels/need-code) : if you are ready to jump in with code.
* [need-advice](https://github.com/UprootLabs/gngr/labels/need-advice) : if you have expertise on the topic.
* [need-triage](https://github.com/UprootLabs/gngr/labels/need-triage) : if you would like to help test or analyse a particular issue.

Ofcourse, you are welcome to take up something that is not listed above, but please co-ordinate with us first
before you spend effort on it.

You can also contribute to our [upstream projects](https://gngr.info/doc/credits.html).

### Developer Certification of Origin (DCO)
Licensing is very important to open source projects. It helps ensure the software continues to be available under the terms that the author desired.

Gngr uses GPL and LGPL licenses to strike a balance between open contribution and allowing you to use the software however you would like to.

The license tells you what rights you have that are provided by the copyright holder. It is important that the contributor fully understands what rights they are licensing and agrees to them. Sometimes the copyright holder isn't the contributor, such as when the contributor is doing work on behalf of a company.

To make a good faith effort to ensure these criteria are met, `gngr` requires the Developer Certificate of Origin (DCO) process to be followed.

The DCO is an attestation attached to every contribution made by every developer. In the commit message of the contribution, the developer simply adds a Signed-off-by statement and thereby agrees to the DCO, which you can find below or at <http://developercertificate.org/>.

```
Developer's Certificate of Origin 1.1

By making a contribution to this project, I certify that:

(a) The contribution was created in whole or in part by me and I
    have the right to submit it under the open source license
    indicated in the file; or

(b) The contribution is based upon previous work that, to the
    best of my knowledge, is covered under an appropriate open
    source license and I have the right under that license to   
    submit that work with modifications, whether created in whole
    or in part by me, under the same open source license (unless
    I am permitted to submit under a different license), as
    Indicated in the file; or

(c) The contribution was provided directly to me by some other
    person who certified (a), (b) or (c) and I have not modified
    it.

(d) I understand and agree that this project and the contribution
    are public and that a record of the contribution (including
    all personal information I submit with it, including my
    sign-off) is maintained indefinitely and may be redistributed
    consistent with this project or the open source license(s)
    involved.
```

For more information on the change see the Gngr Blog post (TODO)

#### DCO Sign-Off Methods

The DCO requires a sign-off message in the following format appear on each commit in the pull request:

```
Signed-off-by: XXXX <XXXX@xyz.com>
```

The DCO text can either be manually added to your commit body, or you can add either **-s** or **--signoff** to your usual git commit commands. If you forget to add the sign-off you can also amend a previous commit with the sign-off by running **git commit --amend -s**. If you've pushed your changes to Github already you'll need to force push your branch after this with **git push -f**.

To ensure that the commit message has not been tampered with, you also need to [GPG sign your commit](https://help.github.com/articles/signing-commits-using-gpg/) with a verified key.

### Gngr Obvious Fix Policy

Small contributions, such as fixing spelling errors, where the content is small enough to not be considered intellectual property, can be submitted without signing the contribution for the DCO.

As a rule of thumb, changes are obvious fixes if they do not introduce any new functionality or creative thinking. Assuming the change does not affect functionality, some common obvious fix examples include the following:

- Spelling / grammar fixes
- Typo correction, white space and formatting changes
- Comment clean up
- Bug fixes that change default return values or error codes stored in constants
- Adding logging messages or debugging output
- Changes to 'metadata' files like Gemfile, .gitignore, build scripts, etc.
- Moving source files from one directory or package to another

**Whenever you invoke the "obvious fix" rule, please say so in your commit message:**

```
------------------------------------------------------------------------
commit xxxxxxx
Author: yyyyyyyyyy
Date:   Wed Sep 18 11:44:40 2015 -0700

  Fix typo in the README.

  Obvious fix.

------------------------------------------------------------------------
```
