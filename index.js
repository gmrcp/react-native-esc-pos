import { NativeModules } from 'react-native';

const { LayoutBuilder } = NativeModules;

const EscPos = {
  ...NativeModules.EscPos
};

export { EscPos, LayoutBuilder };
export default EscPos;
