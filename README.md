# Monorepo Starter

This is a polyglot monorepo boilerplate for The Palmer Group. It is a starter monorepo comprised of TypeScript apps/packages, placeholder JVM and Python apps and an example deployment workflow.

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->

- [Overview](#overview)
  - [What's inside](#whats-inside)
- [Tweaking for your project](#tweaking-for-your-project)
- [TypeScript](#typescript)
  - [Referencing packages from other packages/apps](#referencing-packages-from-other-packagesapps)
  - [Installing](#installing)
  - [Development](#development)
  - [Package Management](#package-management)
    - [Installing a module from Yarn](#installing-a-module-from-yarn)
    - [Uninstalling a module from a package](#uninstalling-a-module-from-a-package)
  - [Package Versioning and TS Paths](#package-versioning-and-ts-paths)
    - [Altering paths and package names](#altering-paths-and-package-names)
- [JVM](#jvm)
  - [Kotlin](#kotlin)
- [Python](#python)
- [Inspiration](#inspiration)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Overview

The repository is powered by [Lerna](https://github.com/lerna/lerna) and [Yarn](https://yarnpkg.com/en/). Lerna is reponsible for bootstrapping, installing, symlinking all of the packages/apps together.

### What's inside

This repo includes multiple packages and applications for a hypothetical project called `mono`. Here's a rundown of the folders:

- `mono-common`: Shared utilities (TypeScript)
- `mono-ui`: Component library (TypeScript x Storybook) (depends on `mono-common`)
- `mono-cra`: Create React App x TypeScript (depends on `mono-common` + `mono-ui`)
- `mono-razzle`: Razzle x TypeScript (depends on `mono-common` + `mono-ui`)
- `.circle`: Some example CircleCI v2 workflows for various monorepo setups (including polyglot)

## Tweaking for your project

You should run a search and replace on the word `mono` and replace with your project name. Rename folders from `mono-xxx` as well. Lastly, `-razzle` and `-cra` are just placeholders. You should carefully replace them with their proper names as well like `-web`,`-admin`, `-api`, or `-whatever-makes-sense`.

## TypeScript

### Referencing packages from other packages/apps

Each package can be referenced within other packages/app files by importing from `@<name>/<folder>` (kind of like an npm scoped package).

```tsx
import * as React from 'react';
import './App.css';
import { Button } from '@mono/ui';

class App extends React.Component<any> {
  render() {
    return (
      <div className="App">
        <header className="App-header">
          <h1 className="App-title">Welcome to React</h1>
        </header>
        <Button>Hello</Button>
        <p className="App-intro">
          To get started, edit <code>src/App.tsx</code> and save to reload.
        </p>
      </div>
    );
  }
}

export default App;
```

**IMPORTANT: YOU DO NOT NEED TO CREATE/OWN THE NPM ORGANIZATION OF YOUR PROJECT NAME BECAUSE NOTHING IS EVER PUBLISHED TO NPM.**

For more info, see the section on [package versioning](#package-versioning-and-ts-paths)

### Installing

Install lerna globally.

```
npm i -g lerna
```

```
git clone git@github.com:palmerhq/typescript-monorepo-starter.git
cd typescript-monorepo-starter
rm -rf .git
yarn install
```

### Development

Lerna allows some pretty nifty development stuff. Here's a quick rundown of stuff you can do:

- _`yarn start`_: Run's the `yarn start` command in every project and pipe output to the console. This will do the following:
  - mono-cra: Starts the app in dev mode on port 3000
  - mono-razzle: Starts the app in dev mode on port 8082
  - mono-ui: Starts TypeScript watch task, and also launches react-storybook on port 6006
  - mono-common: Starts TypeScript watch task
- _`yarn test`_: Run's the `yarn test` command in every project and pipe output to the console. Because of how jest works, this cannot be run in watch mode...womp.womp.
- _`yarn build`_: Build all projects
- _`lerna clean`_: Clean up all node_modules
- _`lerna bootstrap`_: Rerun lerna's bootstrap command

### Package Management

\*\*IF you run `yarn add <module>` or `npm install <module>` from inside a project folder, you will break your symlinks.\*\* To manage package modules, please refer to the following instructions:

#### Installing a module from Yarn

To add a new npm module to ALL packages, run

```bash
lerna add <module>
```

To add a new npm module(s) to just one package

```bash
lerna add <module> --scope=<package-name> <other yarn-flags>

# Examples (if your project name was `mono`)
lerna add classnames --scope=@mono/ui
lerna add @types/classnames @types/jest --scope=@mono/ui --dev
```

#### Uninstalling a module from a package

Unfortunately, there is no `lerna remove` or `lerna uninstall` command. Instead, you should remove the target module from the relevant package or packages' `package.json` and then run `lerna bootstrap` again.

Reference issue: https://github.com/lerna/lerna/issues/1229#issuecomment-359508643

### Package Versioning and TS Paths

None of the packages in this setup are _ever_ published to NPM. Instead, each shared packages' (like `mono-common` and `mono-ui`) have build steps (which are run via `yarn prepare`) and get built locally and then symlinked. This symlinking solves some problems often faced with monorepos:

- All projects/apps are always on the latest version of other packages in the monorepo
- You don't actually need to version things (unless you actually want to publish to NPM)
- You don't need to do special stuff in the CI or Cloud to work with private NPM packages

Somewhat confusingly, (and amazingly), the TypeScript setup for this thing for both `mono-cra` and `mono-razzle` directly reference source code (`<name>/src/**`) of `mono-ui` and `mono-common` by messing with `paths` in [`tsconfig.json`](./tsconfig.json).

```json
{
  "extends": "./tsconfig.base.json",
  "compilerOptions": {
    "baseUrl": ".",
    "paths": {
      "@mono/common/*": ["./mono/common/src"],
      "@mono/ui/*": ["./mono/ui/src"]
    }
  }
}
```

Long story short, this means that you can open up this whole thing at the root directory and VSCode will understand what is going on.

You are welcome.

#### Altering paths and package names

If you don't like the `@mono/<folder>` you can change it to whatever you want to. For example, you may want to change it to `mono-<folder>` so it exactly matches the folder names.

To do this, you need to (search and replace `@mono/` with `mono-`). Or in other words:

- Edit `./tsconfig.json`'s `paths`
- Edit each package's `name` in `package.json`
- Update references to related packages in `dependencies` in each `package.json`
- Update references in code in each package.

Again, search and replace will work.

**Important: If you do this, then your Lerna installation scoping will also change because it uses the package name.**

```bash
# old
lerna add axios --scope=@mono/common

# new (changed to mono-common)
lerna add axios --scope=mono-common
```

## JVM

### Kotlin

Not open source yet. Refer to internal Palmer Group documentation. Deployment workflow for the `mono-jvm` folder is a stub.

## Python

Not open source yet. Refer to internal Palmer Group documentation. Deployment workflow for the `mono-python` folder is a stub.

## Inspiration

A LOT of this has been shameless taken from [Quramy/lerna-yarn-workspaces-example](https://github.com/Quramy/lerna-yarn-workspaces-example). So much so, in fact, you can read the original README.md in [WORKSPACES.md](./WORKSPACES.md)
