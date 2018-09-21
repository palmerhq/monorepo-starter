# TypeScript Monorepo Starter

This is a TypeScript monorepo boilerplate for The Palmer Group. It is a monorepo comprised of apps and packages used to power internal and client projects.

## Overview

The repository is powered by [Lerna](https://github.com/lerna/lerna) and [Yarn](https://yarnpkg.com/en/). Lerna is reponsible for bootstrapping, installing, symlinking all of the packages together.

## Package Management

\*\*IF you run `yarn add <module>` or `npm install <module>` from inside a project folder, you will break your symlinks.\*\* To manage package modules, please refer to the following instructions:

### Installing a module from Yarn

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

### Uninstalling a module from a package

Unfortunately, there is no `lerna remove` or `lerna uninstall` command. Instead, you should remove the target module from the relevant package or packages' `package.json` and then run `lerna bootstrap` again.

Reference issue: https://github.com/lerna/lerna/issues/1229#issuecomment-359508643

## Installing

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

## Tweaking for your project

You should run a search and replace on the word `mono` and replace with your project name. Rename folders from `mono-xxx` as well. Lastly, `-razzle` and `-cra` are just placeholders. You should carefully replace them with their proper names as well like `-web` or `-admin` or `-whatever-makes-sense`.

## Development

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
