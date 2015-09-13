gngr
====

This is the source code of [gngr](https://gngr.info), a new cross-platform browser that champions privacy. `gngr`
is an independent implementation of web standards in pure Java. It is *not* a wrapper around other browser engines.

`gngr` plans to protect privacy by
* disabling the following by default: Cookies, Javascript, XHR requests and providing fine granuality of control over them.
* by implementing in a high-level language that is easier to audit
* by using run-time sandboxing

Read [the introduction](https://gngr.info/doc/introduction.html) to know more.

![gngr Inception](https://gngr.info/media/img/screens/v03.10/gngrInception.png)

### Status
This is an early prototype that is usable with simple websites.

Security-wise, the code isn't hardened *at all!* Use the current version:
 * with [app level sandboxing](https://github.com/UprootLabs/gngr/wiki/App-Sandboxing) 
 * with simple, trusted web-sites only
 * periodically [clear the browser cache and data](https://github.com/UprootLabs/gngr/wiki/Clearing-cache-and-data).

**Do Not use the current version with critical websites such as banking websites or web-mail.**

### Contributing

Everyone is welcome to contribute. Issues that can be easily picked up are marked with the
following tags:
* [need-code](https://github.com/UprootLabs/gngr/labels/need-code) : if you are ready to jump in with code.
* [need-advice](https://github.com/UprootLabs/gngr/labels/need-advice) : if you have expertise on the topic.
* [need-triage](https://github.com/UprootLabs/gngr/labels/need-triage) : if you would like to help test or analyse a particular issue.

Ofcourse, you are welcome to take up something that is not listed above, but please co-ordinate with us first
before you spend effort on it.

See [the contributing guide](CONTRIBUTING.md) for more details.

### Ways to reach us
  * IRC: `#gngr` and `#gngr-dev` on freenode.
  * Reddit: [/r/gngr](https://reddit.com/r/gngr)
  * [![Gitter](https://badges.gitter.im/UprootLabs/gngr.png)](https://gitter.im/UprootLabs/gngr?utm_source=badge)
  * [Our blog](https://blog.gngr.info) and its [feed](https://blog.gngr.info/feed.xml)

### License & Copyright
[GPLv2](https://www.gnu.org/licenses/gpl-2.0.html)

 * Copyright 2009 Lobo Project
 * Copyright 2014, 2015 Uproot Labs India Pvt Ltd

### Pre-History
`gngr` began as a fork of the now-defunct project called the `lobo` browser (see [credits](https://gngr.info/doc/credits.html)).
For archival purposes, the history of all commits that transform `lobo` code from its CVS repository to the very first version of gngr is available in [a separate repository](https://github.com/UprootLabs/gngrPreHistoric).
