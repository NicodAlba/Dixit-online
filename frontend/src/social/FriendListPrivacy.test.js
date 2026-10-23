import React from 'react';
import {render, screen, fireEvent, waitFor} from '@testing-library/react';
import {MemoryRouter, Routes, Route} from 'react-router-dom';
import {FriendList} from './FriendList';
import useFetchState from '../util/useFetchState';
jest.mock('../util/useFetchState');
jest.mock('../services/token.service', () => ({getLocalAccessToken: () => 'fixture-token',getUser:()=>({id:1})}));
function show(friends) {
 useFetchState.mockReturnValue([friends,jest.fn()]);
 render(<MemoryRouter><Routes><Route path="/" element={<FriendList/>}/><Route path="/games/42/spectate/17" element={<div>Watching fixture</div>}/></Routes></MemoryRouter>);
}
afterEach(()=>jest.resetAllMocks());
test('safe friend without private participation graph still renders',()=>{
 show([{id:2,username:'Friend'}]);
 expect(screen.getByText('Friend')).toBeInTheDocument();
 expect(screen.queryByRole('button',{name:'Spectate'})).not.toBeInTheDocument();
 expect(useFetchState).toHaveBeenCalledTimes(1);
 expect(useFetchState.mock.calls[0][1]).toBe('/api/v1/friends');
});
test('authorized spectator link uses only safe game and player identifiers',async()=>{
 global.fetch=jest.fn().mockResolvedValue({ok:true});
 show([{id:2,username:'Friend',spectatableGame:{id:42,playerId:17}}]);
 fireEvent.click(screen.getByRole('button',{name:'Spectate'}));
 await waitFor(()=>expect(screen.getByText('Watching fixture')).toBeInTheDocument());
 expect(fetch).toHaveBeenCalledWith('/api/v1/games/42/spectate/17',expect.objectContaining({method:'PATCH'}));
});
test('server denial is shown and does not enter spectator mode',async()=>{
 global.fetch=jest.fn().mockResolvedValue({ok:false,status:403,json:async()=>({message:'Not eligible'})});
 window.alert=jest.fn();
 show([{id:2,username:'Friend',spectatableGame:{id:42,playerId:17}}]);
 fireEvent.click(screen.getByRole('button',{name:'Spectate'}));
 await waitFor(()=>expect(window.alert).toHaveBeenCalledWith('You cannot spectate this match: Not eligible'));
 expect(screen.queryByText('Watching fixture')).not.toBeInTheDocument();
});
