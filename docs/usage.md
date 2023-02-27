# Usage

To use the library, first add the `mavenCentral` repository to your `repositories` block in your `build.gradle`:
```groovy
repositories {
  mavenCentral()
}
```
Then, add the `tw-base-utils` library as a dependency in your `dependencies` block in your `build.gradle`:
```groovy
dependencies {
  implementation 'com.transferwise.common:tw-base-utils:<VERSION>'
}
```
> Replace `<VERSION>` with the version of the library you want to use.

And that's it, you now have access to the full-suite of Wise's base utilities.