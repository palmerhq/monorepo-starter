import * as React from 'react';
import * as ReactDOM from 'react-dom';

import { Button } from './Button';
import { MemoryRouter } from 'react-router-dom';

describe('<Button />', () => {
  test('renders without exploding', () => {
    const div = document.createElement('div');
    ReactDOM.render(
      <MemoryRouter>
        <Button>Test</Button>
      </MemoryRouter>,
      div
    );
  });
});
