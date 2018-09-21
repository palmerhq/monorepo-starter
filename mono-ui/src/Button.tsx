import * as React from 'react';

export interface ButtonProps {
  children: any;
  onClick?: () => void;
}

export interface ButtonState {}
export class Button extends React.Component<ButtonProps, ButtonState> {
  render() {
    return <button {...this.props} />;
  }
}
