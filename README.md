# gson-utils

Gson-utils makes it easier to serialize and deserialize certain immutable types
provided by Google Guava.

## Why?

Serializing and deserializing immutable value classes full of simple fields
is pretty straightforward with Gson, but it doesn't take much more complication
in contained data to suddenly demand more fiddliness to deal with.

Various other GitHub users have made projects to help with this, but each of the
ones I found in searches either had room for improvement or a quirk that made
them less usable to me:
Maybe they didn't guarantee the sort order of the deserialized data would match
the original.  Or maybe they didn't support a basic, important collection type
such as Set.  Or maybe they didn't "deeply" serialize Collections of objects.
Or maybe a project I wanted to contribute to has its own particular restrictions
on dependencies or language features (reflection, injection, annotations, etc.)
If you do want to look into the projects I browsed through, they are:
* acebaggins/gson-serializers
* dampcake/gson-immutable
* rharter/gson-autovalue
* google/caliper's com.google.caliper.json package

## How?

Given that this project's code is meant to be used in conjunction with both
Google Guava's collections and Google Gson, the package contained in Google
Caliper seemed like the most approprite starting point.
This project then strips away some of that package's less essential-looking
dependencies, resolves the sort order guarantees issue, and adds support for
ImmutableSet.