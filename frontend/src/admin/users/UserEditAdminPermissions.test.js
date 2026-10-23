import React from 'react';
import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import UserEditAdmin from './UserEditAdmin';
import useFetchState from '../../util/useFetchState';
jest.mock('../../util/useFetchState');
jest.mock('../../util/useFetchData', () => () => [{id:1, authority:'PLAYER'}, {id:2, authority:'ADMIN'}]);
jest.mock('../../util/getIdFromUrl', () => () => '2');
jest.mock('../../services/token.service', () => ({getLocalAccessToken: () => 'test-token', getUser: () => ({id:1})}));
jest.mock('../../util/getErrorModal', () => () => null);

function show(id) {
  useFetchState.mockReturnValue([{id, username:'Fixture', authority:{id:1}}, jest.fn()]);
  render(<MemoryRouter><UserEditAdmin /></MemoryRouter>);
}
test('admin cannot enter a new password for another existing account', () => {
  show(2);
  expect(screen.queryByLabelText('Password')).not.toBeInTheDocument();
  expect(screen.getByLabelText('Authority')).toBeInTheDocument();
});
test('admin can enter their own new password', () => {
  show(1);
  expect(screen.getByLabelText('Password')).toBeInTheDocument();
});
test('admin can set a password when creating an account', () => {
  show(null);
  expect(screen.getByLabelText('Password')).toBeRequired();
});
