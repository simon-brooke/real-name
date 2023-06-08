# real-name

A Clojure library designed to find the real name of a user, given the user name.

## Rationale

Surprisingly, in the whole Java ecosystem there doesn't seem to be (or at least
I could not find) any well-known library to find the real ordinary name of a
given user. Every modern operating system has some way to do this, but there's
no unified interface.

Until now.

This is a very simple library that essentially does one thing: it finds the real
name of a user on the current machine, given the user name.

**Warning!** this is essentially a bundle of hacks. I don't approve of it, but
there did not seem to be any other ways to do it.

## Usage

If building from Leiningen, add to project dependencies

```clojure
[cc.journeyman/real-name "0.0.1"]
```

Add the following requirement to your namespace:

```clojure
(ns your.fine.namespace
  (:require [cc.journeyman.real-name :refer [get-real-name]]))
```

Resolve usernames to real names by invoking `(get-real-name)`, either without 
arguments to get the real name of the current user:

```clojure
cc.journeyman.real-name.core=> (get-real-name )
"Simon Brooke"
```

Or with one string argument, a username, to find the real name of the named user:

```clojure
cc.journeyman.real-name.core=> (get-real-name "simon")
"Simon Brooke"
```

## Errors

If it doesn't work, an `ex-info` will be thrown, with at least the keys `:username` and `:os-name`, and a (hopefully useful) diagnostic message:

```clojure
cc.journeyman.real-name.core=> (get-real-name "fred")
Execution error (ExceptionInfo) at cc.journeyman.real-name.core/mac-os-x (core.clj:13).
Real name for `fred` not found because `id: fred: no such user`.

cc.journeyman.real-name.core=> (ex-data *e)
{:username "fred", :error "id: fred: no such user\n", :os-name "Mac OS X", 
 :os-version "10.15.7"}
```

## Intended operating systems

It's intended that this will work on all reasonably common operating systems
which Java supports. Specifically I'm hoping that it will work on:

* Linux, generally;
* Mac OS X, generally;
* UN*X variants based on BSD and/or System V.4;
* Windows variants from Windows 7 onwards.

However it's tested only on Mac OS X 10.15.7, Debian 9, Ubuntu 23.04, and Windows 10.

If it does not work, please raise an issue on this project, giving the full `ex-data`
from the exception you experienced. I don't guarantee I can fix it, because I don't
have access to all platforms to test, but I will do my best.

## Invoking from Java

As there doesn't seem to be a Java-native solution, you may want to use this
library from Java. I intend that you should be able to do so, but this is work
in progress. The following should work:

```java
import cc.journeyman.real_name;

public class Main {

    public static void main(String[] args) {
        System.out.println( real_name.getRealName(args[1]);
    }
}
```

## License

Copyright © 2023 Simon Brooke

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
