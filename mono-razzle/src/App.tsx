import * as React from 'react';
import { Route, Switch } from 'react-router-dom';
import Home from './Home';

import './App.css';
import { Button } from '@mono/ui';
import { throttle } from '@mono/common';

throttle(() => console.log('hello'), 20);

const App = () => (
  <div>
    <Button>Hello</Button>
    <Switch>
      <Route exact={true} path="/" component={Home} />
    </Switch>
  </div>
);

export default App;
