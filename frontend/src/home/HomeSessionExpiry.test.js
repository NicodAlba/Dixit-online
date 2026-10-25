jest.mock("./ActiveGames", () => () => null);
import React from 'react';
import {act,fireEvent,render,screen,waitFor} from '@testing-library/react';
import {MemoryRouter,Routes,Route} from 'react-router-dom';
import Home from './index';
import {GameInvitationList} from './GameInvitationList';
jest.mock('jwt-decode',()=>()=>({authorities:['PLAYER']}));
const originalLocation=window.location,originalFetch=global.fetch;
const reply=(data,status=200)=>({ok:status>=200&&status<300,status,headers:{get:()=> 'application/json'},json:async()=>data});
function home(){return <MemoryRouter><Routes><Route path="/" element={<Home/>}/><Route path="/login" element={<h1>Login page</h1>}/></Routes></MemoryRouter>;}
beforeAll(()=>{delete window.location;window.location={...originalLocation,replace:jest.fn()};});
afterAll(()=>{window.location=originalLocation;});
beforeEach(()=>{localStorage.clear();localStorage.setItem('jwt',JSON.stringify('old-token'));localStorage.setItem('user',JSON.stringify({id:1,username:'Fixture'}));global.fetch=jest.fn();window.location.replace.mockClear();});
afterEach(()=>{localStorage.clear();global.fetch=originalFetch;});
test('rejected stored session is cleared and public home can reach login without another private request',async()=>{
 global.fetch.mockResolvedValue(reply({message:'Full authentication is required to access this resource.'},401));
 const view=render(home());
 await waitFor(()=>expect(localStorage.getItem('jwt')).toBeNull());
 expect(localStorage.getItem('user')).toBeNull();expect(window.location.replace).toHaveBeenCalledWith('/');
 expect(screen.queryByText('Full authentication is required to access this resource.')).not.toBeInTheDocument();
 view.unmount();render(home());
 fireEvent.click(screen.getByRole('button',{name:'Play game'}));
 await screen.findByRole('dialog',{name:'Join the story'});
 fireEvent.click(screen.getByRole('button',{name:'Log in',exact:true}));
 expect(screen.getByRole('heading',{name:'Login page'})).toBeInTheDocument();expect(fetch).toHaveBeenCalledTimes(1);
});
test('old 401 cannot clear a newer session',async()=>{
 let finish;global.fetch.mockReturnValue(new Promise(resolve=>{finish=resolve;}));
 render(<MemoryRouter><GameInvitationList/></MemoryRouter>);
 await waitFor(()=>expect(fetch).toHaveBeenCalledTimes(1));
 localStorage.setItem('jwt',JSON.stringify('new-token'));
 await act(async()=>{finish(reply({message:'Rejected old session'},401));});
 expect(localStorage.getItem('jwt')).toBe(JSON.stringify('new-token'));expect(window.location.replace).not.toHaveBeenCalled();
 expect(screen.queryByRole('alert')).not.toBeInTheDocument();
});
test('expired session during explicit acceptance leads to login',async()=>{
 global.fetch.mockResolvedValueOnce(reply([{id:7,gameId:42,inviter:'host'}])).mockResolvedValueOnce(reply({message:'Full authentication is required to access this resource.'},401));
 render(home());fireEvent.click(await screen.findByRole('button',{name:'Accept'}));
 await waitFor(()=>expect(window.location.replace).toHaveBeenCalledWith('/login'));
 expect(localStorage.getItem('jwt')).toBeNull();
});
